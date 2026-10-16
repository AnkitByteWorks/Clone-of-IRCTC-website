package com.irctc.railconnect.catalog.repository;

import com.irctc.railconnect.catalog.document.StationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends MongoRepository<StationDocument, String> {

    Optional<StationDocument> findByStationCodeIgnoreCase(String stationCode);

    boolean existsByStationCodeIgnoreCase(String stationCode);

    @Query("{ 'active': true, $or: [ " +
           "{ 'stationCode': { $regex: ?0, $options: 'i' } }, " +
           "{ 'stationName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'city': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<StationDocument> searchStations(String query);
}
