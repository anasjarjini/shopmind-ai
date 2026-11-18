
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from recommendation.feature_extractor import ( build_product_document)


class TfidfEngine:

    def __init__(self, products: list[dict]):

        if not products:
            raise ValueError("No products available for AI recommendations")

        self.products = products

        # Map PostgreSQL product IDs to matrix row indices
        self.id_to_index = {
            int(product["id"]): index for index, product in enumerate(products)
        }

        if len(self.id_to_index) != len(products):
            raise ValueError( "Duplicate product IDs in recommendation data")

        # Build text document for every product
        self.documents = [
            build_product_document(product)
            for product in products
        ]

        if not any(document.strip() for document in self.documents):
            raise ValueError("Product documents are empty")

        # Configure the TF-IDF algorithm
        self.vectorizer = TfidfVectorizer(
            stop_words="english",
            ngram_range=(1, 2),
            max_features=10000,
            sublinear_tf=True,
            strip_accents="unicode",
            norm="l2"
        )

        # Calculate TF-IDF vectors
        self.tfidf_matrix = self.vectorizer.fit_transform(self.documents)

        print(
            f"TF-IDF initialized: "
            f"{self.tfidf_matrix.shape[0]} products, "
            f"{self.tfidf_matrix.shape[1]} features"
        )


    def recommend(self,product_id: int,top_n: int = 5,same_category: bool = True) -> list[dict]:

        if top_n < 1:
            raise ValueError("top_n must be greater than zero")

        if product_id not in self.id_to_index:
            raise KeyError(f"Product {product_id} not found")

        # Find target product row
        target_index = self.id_to_index[product_id]

        target_product = self.products[target_index]

        # Calculate cosine similarity
        similarity_scores = cosine_similarity(self.tfidf_matrix[target_index],self.tfidf_matrix).flatten()

        candidates = []

        # Compare the target with other products
        for index, product in enumerate(self.products):

            current_product_id = int(product["id"])

            # Do not recommend the same product
            if current_product_id == product_id:
                continue

            # Recommend only from the same category
            if same_category:
                if ( product["category_id"] != target_product["category_id"]):
                    continue

            score = float(similarity_scores[index])

            # Ignore products with zero similarity
            if score <= 0:
                continue

            candidates.append({
                "productId": current_product_id,
                "name": product["name"],
                "brand": product["brand"],
                "category": product["category_name"],
                "similarityScore": round(score, 4)
            })

        # Sort by highest similarity
        candidates.sort(
            key=lambda product: (-product["similarityScore"], product["productId"])
        )

        return candidates[:top_n]


    def get_model_info(self) -> dict:

        return {
            "algorithm": "TF-IDF + Cosine Similarity",
            "totalProducts": len(self.products),
            "totalFeatures": self.tfidf_matrix.shape[1],
            "matrixShape": list(self.tfidf_matrix.shape)
        }
