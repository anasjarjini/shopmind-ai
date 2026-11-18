
from threading import Lock
from sqlalchemy.orm import Session

from repositories.product_repository import ProductRepository
from recommendation.tfidf_engine import TfidfEngine


class RecommendationService:

    def __init__(self):

        # TF-IDF model stored in RAM
        self._engine: TfidfEngine | None = None

        # Prevent simultaneous model initialization
        self._lock = Lock()


    def _build_engine(self, db: Session) -> TfidfEngine:

        # Retrieve products from PostgreSQL
        products = (ProductRepository.get_products_for_recommendation(db))

        print(f"Loading {len(products)} products "f"from PostgreSQL")

        # Calculate TF-IDF vectors
        return TfidfEngine(products)


    def get_engine(self, db: Session) -> TfidfEngine:

        # Reuse model if already initialized
        if self._engine is not None:
            return self._engine

        # Only one thread can initialize the model
        with self._lock:

            if self._engine is None:
                self._engine = self._build_engine(db)

            return self._engine


    def recommend(self, db: Session,product_id: int,top_n: int = 5,same_category: bool = True) -> list[dict]:

        engine = self.get_engine(db)

        return engine.recommend(
            product_id=product_id,
            top_n=top_n,
            same_category=same_category
        )


    def rebuild(self, db: Session) -> dict:

        # Build a new model first
        new_engine = self._build_engine(db)

        # Replace old model atomically
        with self._lock:
            self._engine = new_engine

        return {
            "status": "success",
            "message": "AI model rebuilt successfully",
            "model": new_engine.get_model_info()
        }


    def get_model_info(self) -> dict:

        engine = self._engine

        if engine is None:
            return {
                "status": "not_initialized",
                "message": "Model will load on first request"
            }

        return {
            "status": "ready",
            **engine.get_model_info()
        }


# Shared model instance for this Python process
recommendation_service = RecommendationService()
