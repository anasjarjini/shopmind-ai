from fastapi import FastAPI

app = FastAPI(title="ShopMind AI Service", version="0.0.1")


@app.get("/health")
def health():
    return {"status": "ok"}


# TODO: ajouter les routes de recommandation
