import requests
import pandas as pd

def analyze_violations():
    print("Starting the analyze_violations function...")  # Debug line

    # Fetch violations data from the Spring Boot API
    try:
        print("Sending request to API...")  # Debug line
        response = requests.get('http://localhost:8080/violations')
        print(f"Received response with status code: {response.status_code}")  # Debug line

        if response.status_code == 200:
            violations = response.json()
            print("Data successfully fetched from API")  # Debug line

            # Convert to DataFrame for analysis
            df = pd.DataFrame(violations)
            print("Data converted to DataFrame")  # Debug line

            # Basic Analysis: Most Common Violation Type
            most_common = df['violationType'].value_counts().idxmax()
            total_violations = df.shape[0]
            print("Analysis complete")  # Debug line

            return {
                "most_common_violation": most_common,
                "total_violations": total_violations
            }
        else:
            print("Failed to fetch data, status code not 200")  # Debug line
            return {"error": "Failed to fetch data"}
    
    except Exception as e:
        print(f"An error occurred: {e}")  # Debug line
        return {"error": f"Exception encountered: {e}"}

# Run the function if this file is executed directly
if __name__ == "__main__":
    print("Running the script...")  # Debug line
    result = analyze_violations()
    print("Function output:", result)  # Debug line
