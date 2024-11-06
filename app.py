from flask import Flask, jsonify, request
from traffic_violation_model import analyze_violations

app = Flask(__name__)

@app.route('/api/analyze', methods=['POST'])  # Change to POST
def analyze():
    print("Received POST request to /api/analyze")  # Debug line
    try:
        # This will call the analysis function and return results
        result = analyze_violations()
        print("Analysis complete, sending result")  # Debug line
        return jsonify(result)
    except Exception as e:
        print(f"Error during analysis: {e}")  # Debug line for error
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("Starting Flask application...")  # Debug line before starting app
    try:
        app.run(debug=True, port=5000)
    except Exception as e:
        print(f"Error while running Flask app: {e}")  # Debug line for Flask app error
