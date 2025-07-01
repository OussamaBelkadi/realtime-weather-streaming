package com.example.demo;


import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.KeyValue;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class WeatherStreamProcessor {
    @Bean
    public KStream<String, String> processWeatherData(StreamsBuilder builder) {
        KStream<String, String> weatherStream = builder.stream("weather-data");

        // 1. Filtrer les températures supérieures à 30°C
        KStream<String, String> filtered = weatherStream
                .mapValues(value -> value.trim())
                .filter((key, value) -> {
                    String[] parts = value.split(",");
                    return parts.length == 3 && Double.parseDouble(parts[1]) > 30.0;
                });

        // 2. Convertir en Fahrenheit
        KStream<String, String> converted = filtered.mapValues(value -> {
            String[] parts = value.split(",");
            String station = parts[0];
            double celsius = Double.parseDouble(parts[1]);
            int humidity = Integer.parseInt(parts[2]);
            double fahrenheit = (celsius * 9 / 5) + 32;
            return station + "," + fahrenheit + "," + humidity;
        });

        // 3. Regrouper par station
        KGroupedStream<String, String> grouped = converted
                .map((key, value) -> {
                    String[] parts = value.split(",");
                    return KeyValue.pair(parts[0], value);
                })
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()));

        // 4. Agréger (température et humidité moyennes)
        KTable<String, String> aggregated = grouped.aggregate(
                () -> "0.0,0,0", // totalTemp, totalHumidity, count
                (station, newValue, aggregate) -> {
                    String[] newParts = newValue.split(",");
                    String[] aggParts = aggregate.split(",");

                    double tempSum = Double.parseDouble(aggParts[0]) + Double.parseDouble(newParts[1]);
                    int humiditySum = Integer.parseInt(aggParts[1]) + Integer.parseInt(newParts[2]);
                    int count = Integer.parseInt(aggParts[2]) + 1;

                    return tempSum + "," + humiditySum + "," + count;
                },
                Materialized.with(Serdes.String(), Serdes.String())
        ).mapValues(aggValue -> {
            String[] parts = aggValue.split(",");
            double avgTemp = Double.parseDouble(parts[0]) / Integer.parseInt(parts[2]);
            double avgHumidity = (double) Integer.parseInt(parts[1]) / Integer.parseInt(parts[2]);
            return String.format("Température Moyenne = %.2f°F, Humidité Moyenne = %.2f%%", avgTemp, avgHumidity);
        });

        // 5. Publier dans le topic station-averages
        aggregated.toStream().to("station-averages", Produced.with(Serdes.String(), Serdes.String()));

        // On peut retourner le flux initial ou null si inutile
        return weatherStream;
    }
}
