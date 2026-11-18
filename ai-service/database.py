
import os
import logging

from dotenv import load_dotenv

from sqlalchemy import create_engine, text
from sqlalchemy.engine import URL
from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import SQLAlchemyError

load_dotenv()

logger = logging.getLogger(__name__)



DATABASE_URL = URL.create(
    drivername="postgresql+psycopg2",

    username=os.getenv(
        "DATABASE_USER", "shopmind"
    ),

    password=os.getenv(
        "DATABASE_PASSWORD", "shopmind"
    ),

    host=os.getenv(
        "DATABASE_HOST", "postgres"
    ),

    port=int(os.getenv(
        "DATABASE_PORT", "5432"
    )),

    database=os.getenv(
        "DATABASE_NAME", "shopmind"
    )
)


engine = create_engine(
    DATABASE_URL,

    pool_pre_ping=True,

    pool_size=5,

    max_overflow=5,

    pool_recycle=1800,

    echo=False
)


# ==========================================
# DATABASE SESSION
# ==========================================

SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)



def get_db():

    db = SessionLocal()

    try:
        yield db

    finally:
        db.close()



def test_database_connection() -> bool:

    try:

        with engine.connect() as connection:

            connection.execute(text("SELECT 1"))

        return True

    except SQLAlchemyError:

        logger.exception(
            "PostgreSQL connection failed"
        )

        return False
