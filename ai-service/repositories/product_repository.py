
from sqlalchemy import text
from sqlalchemy.orm import Session


class ProductRepository:

    @staticmethod
    def get_all_categories(db: Session) -> list[dict]:

        query = text("""
            SELECT
                id,
                name,
                description
            FROM categories
            ORDER BY id
        """)

        result = db.execute(query).mappings().all()

        return [dict(row) for row in result]


    @staticmethod
    def get_all_products(db: Session) -> list[dict]:

        query = text("""
            SELECT
                p.id,
                p.amazon_asin,
                p.name,
                p.description,
                p.brand,
                p.store,
                p.price,
                p.stock,
                p.active,

                p.category_id,
                c.name AS category_name,
                c.description AS category_description,

                p.amazon_rating,
                p.rating_count,
                p.date_first_available,

                i.image_url AS main_image_url,

                p.created_at,
                p.updated_at

            FROM products p

            LEFT JOIN categories c
                ON c.id = p.category_id

            LEFT JOIN product_images i
                ON i.product_id = p.id
                AND i.position = 1

            WHERE p.active = TRUE

            ORDER BY p.id ASC
        """)

        result = db.execute(query).mappings().all()

        return [dict(row) for row in result]


    @staticmethod
    def get_product_by_id(
        db: Session,
        product_id: int
    ) -> dict | None:

        product_query = text("""
            SELECT
                p.id,
                p.amazon_asin,
                p.name,
                p.description,
                p.brand,
                p.store,
                p.price,
                p.stock,
                p.active,

                p.category_id,
                c.name AS category_name,
                c.description AS category_description,

                p.amazon_rating,
                p.rating_count,
                p.date_first_available,

                p.created_at,
                p.updated_at

            FROM products p

            LEFT JOIN categories c
                ON c.id = p.category_id

            WHERE p.id = :product_id
              AND p.active = TRUE
        """)

        row = db.execute(
            product_query,
            {"product_id": product_id}
        ).mappings().first()

        if row is None:
            return None

        product = dict(row)

        # Load the three images separately
        images_query = text("""
            SELECT
                id,
                image_url,
                position,
                variant,
                source_resolution

            FROM product_images

            WHERE product_id = :product_id

            ORDER BY position ASC
        """)

        images = db.execute(
            images_query,
            {"product_id": product_id}
        ).mappings().all()

        product["images"] = [
            dict(image) for image in images
        ]

        # Main image for convenience
        product["main_image_url"] = next(
            (
                image["image_url"]
                for image in product["images"]
                if image["position"] == 1
            ),
            None
        )

        return product


    @staticmethod
    def get_statistics(db: Session) -> dict:

        query = text("""
            SELECT
                COUNT(*) AS total_products,

                COUNT(*) FILTER (
                    WHERE active = TRUE
                ) AS active_products,

                COUNT(*) FILTER (
                    WHERE active = FALSE
                ) AS inactive_products,

                (
                    SELECT COUNT(*)
                    FROM product_images
                ) AS total_images,

                (
                    SELECT COUNT(*)
                    FROM categories
                ) AS total_categories

            FROM products
        """)

        result = db.execute(query).mappings().one()

        return dict(result)


    @staticmethod
    def get_products_for_recommendation(db: Session) -> list[dict]:

        query = text("""
            SELECT
                p.id,
                p.name,

                COALESCE(
                    p.brand, ''
                ) AS brand,

                COALESCE(
                    p.description, ''
                ) AS description,

                p.category_id,

                c.name AS category_name,

                p.stock

            FROM products p

            INNER JOIN categories c
                ON c.id = p.category_id

            WHERE p.active = TRUE

            ORDER BY p.id ASC
        """)

        result = db.execute(query).mappings().all()

        return [dict(row) for row in result]
