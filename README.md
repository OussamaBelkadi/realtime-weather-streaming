# 🌦️ Kafka Streams Weather Processing

This project is a real-time weather data processing application using **Apache Kafka Streams** and **Spring Boot**.

It consumes data from a Kafka topic, applies several transformations, and publishes the results to another topic.

---

## 📌 Features

- ✅ Reads weather records from Kafka topic `weather-data`
- 🔥 Filters out readings with temperature > 30°C
- 🌡️ Converts temperatures from Celsius to Fahrenheit
- 📊 Groups data by station and calculates:
    - Average temperature (°F)
    - Average humidity (%)
- 🚀 Writes results to topic `station-averages`

---

## 🧪 Example Input / Output

### Input (weather-data):
