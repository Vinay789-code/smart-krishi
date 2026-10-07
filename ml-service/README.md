# Smart Krishi – Python ML Crop Disease Microservice (Option B)

This microservice provides a standalone, local AI inference server for crop leaf disease classification.

## Requirements
* Python 3.9+
* Optional: PyTorch & torchvision for local deep learning inference

## Setup and Running

1. Open a terminal in this directory:
   ```bash
   cd ml-service
   ```

2. (Optional) Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

3. Launch the microservice:
   ```bash
   python app.py
   ```
   The service will start on `http://localhost:5000`.

## Connecting to Spring Boot Backend
In `backend/src/main/resources/application.properties` or environment variables:
```bash
ML_SERVICE_URL=http://localhost:5000/predict
```

When `ML_SERVICE_URL` is set, the Spring Boot backend routes all leaf scans directly to this local Python service.
