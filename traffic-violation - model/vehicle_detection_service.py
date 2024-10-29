from flask import Flask, request, jsonify
import torch
import cv2
import numpy as np

# Initialize Flask app
app = Flask(__name__)

# Load YOLOv5 model (pre-trained model)
model = torch.hub.load('ultralytics/yolov5', 'yolov5s', pretrained=True)

@app.route('/detect', methods=['POST'])
def detect_vehicles():
    """Detect vehicles in a frame sent from the backend."""
    # Get the image from the request
    file = request.files['frame'].read()
    npimg = np.frombuffer(file, np.uint8)
    img = cv2.imdecode(npimg, cv2.IMREAD_COLOR)

    # Run the image through the YOLO model
    results = model(img)

    # Extract bounding boxes and labels
    detected_objects = results.pandas().xyxy[0].to_dict(orient="records")
    
    # Filter only vehicles (e.g., 'car', 'truck', 'bus')
    vehicles = [obj for obj in detected_objects if obj['name'] in ['car', 'truck', 'bus']]

    # Return detected vehicles as JSON response
    return jsonify(vehicles)

if __name__ == '__main__':
    # Run the service on localhost:5000
    app.run(host='0.0.0.0', port=5000)
