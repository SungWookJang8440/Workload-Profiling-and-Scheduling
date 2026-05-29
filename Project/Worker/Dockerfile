FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

# Docker-in-Docker 또는 호스트 Docker 소켓 바인딩을 가정
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"]
