import requests
import pandas as pd

def analyze_violations():
    # Fetch violations data from the Spring Boot API
    response = requests.get('http://localhost:8080/violations')
    
    if response.status_code == 200:
        violations = response.json()
        
        # Convert to DataFrame for analysis
        df = pd.DataFrame(violations)

        # Basic Analysis: Most Common Violation Type
        most_common = df['violationType'].value_counts().idxmax()
        total_violations = df.shape[0]

        return {
            "most_common_violation": most_common,
            "total_violations": total_violations
        }
    else:
        return {"error": "Failed to fetch data"}
