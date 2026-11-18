def build_product_document(product: dict) -> str:

    return " ".join([
        product["name"],
        product["name"],
        product["brand"],
        product["category_name"],
        product["description"]
    ]).strip()