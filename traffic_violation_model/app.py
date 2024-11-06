from flask import Flask, jsonify
from traffic_violation_model import analyze_violations


app = Flask(__name__)

@app.route('/api/analyze', methods=['GET'])
def analyze():
    # This will call the analysis function and return results
    result = analyze_violations()
    return jsonify(result)

if __name__ == '__main__':
    app.run(debug=True, port=5000)
