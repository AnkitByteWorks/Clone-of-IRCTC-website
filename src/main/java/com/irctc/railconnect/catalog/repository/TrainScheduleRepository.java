package com.irctc.railconnect.catalog.repository;

import com.irctc.railconnect.catalog.document.TrainScheduleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainScheduleRepository extends MongoRepository<TrainScheduleDocument, String> {

    Optional<TrainScheduleDocument> findByTrainNumber(String trainNumber);

    boolean existsByTrainNumber(String trainNumber);

    @Query("{ 'active': true, 'routeStops.stationCode': { $all: [?0, ?1] }, 'runningDays': ?2 }")
    List<TrainScheduleDocument> findActiveTrainsContainingBothStopsOnDay(String fromStationCode, String toStationCode, DayOfWeek dayOfWeek);

    @Query("{ 'active': true, 'routeStops.stationCode': { $all: [?0, ?1] } }")
    List<TrainScheduleDocument> findActiveTrainsContainingBothStops(String fromStationCode, String toStationCode);
}
