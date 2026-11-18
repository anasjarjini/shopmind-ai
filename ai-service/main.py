
from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import text

from database import get_db, test_database_connection
from fastapi import Query

from recommendation.recommendation_service import (recommendation_service)
from repositories.product_repository import ProductRepository

app = FastAPI(title="ShopMind AI",description="AI Product Recommendation Microservice",version="1.0.0")


@app.get("/")
def root():

    return {
        "application": "ShopMind AI",
        "message": "AI Recommendation Service is running",
        "status": "success"
    }


@app.get("/health")
def health_check():

    try:

        connected = test_database_connection()

        return {
            "status": "healthy",
            "database": "connected" if connected else "disconnected"
        }

    except Exception as error:

        raise HTTPException(
            status_code=503,
            detail=f"Database connection failed: {str(error)}"
        )


@app.get("/api/v1/categories")
def get_categories(db: Session = Depends(get_db)):

    try:

        categories = ProductRepository.get_all_categories(db)

        return {
            "total": len(categories),
            "categories": categories
        }

    except Exception as error:

        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving categories: {str(error)}"
        )


@app.get("/api/v1/products")
def get_products(db: Session = Depends(get_db)):

    try:

        products = ProductRepository.get_all_products(db)

        return {
            "total": len(products),
            "products": products
        }

    except Exception as error:

        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving products: {str(error)}"
        )


@app.get("/api/v1/products/{product_id}")
def get_product(
    product_id: int,
    db: Session = Depends(get_db)
):

    product = ProductRepository.get_product_by_id(
        db,
        product_id
    )

    if product is None:

        raise HTTPException(
            status_code=404,
            detail="Product not found or inactive"
        )

    return product


@app.get("/api/v1/database/statistics")
def get_database_statistics(
    db: Session = Depends(get_db)
):

    try:

        statistics = ProductRepository.get_statistics(db)

        categories = ProductRepository.get_all_categories(db)

        return {
            "database": "shopmind",
            "statistics": statistics,
            "total_categories": len(categories)
        }

    except Exception as error:

        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving statistics: {str(error)}"
        )



# AI PRODUCT RECOMMENDATIONS

@app.get("/api/v1/recommendations/{product_id}")
def get_recommendations(product_id: int,limit: int = Query(default=5, ge=1, le=20),same_category: bool = True,db: Session = Depends(get_db)):

    try:
        recommendations = recommendation_service.recommend( db=db, product_id=product_id,top_n=limit,same_category=same_category)

        return {
            "productId": product_id,
            "algorithm": "TF-IDF + Cosine Similarity",
            "total": len(recommendations),
            "recommendations": recommendations
        }

    except KeyError:
        raise HTTPException(status_code=404,detail="Product not found in AI index")

    except ValueError as error:
        raise HTTPException(status_code=503,detail=str(error))
