from fastapi import FastAPI

app = FastAPI(title='Buck Watch ML Service')


@app.get('/health')
def health() -> dict[str, str]:
    return {'status': 'ok', 'service': 'buckwatch-ml'}
