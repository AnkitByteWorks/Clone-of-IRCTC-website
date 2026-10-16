package com.irctc.railconnect.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stations")
public class StationDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String stationCode;

    @Indexed
    private String stationName;

    @Indexed
    private String city;

    private String state;

    private String zone;

    private int numberOfPlatforms;

    private boolean active;
}
