package org.example.voronoi.db;

import org.apache.ibatis.annotations.Select;
import org.example.voronoi.model.Station;

import java.util.List;

/** Station table access. */
public interface StationMapper {

    /**
     * The station table of the original data set. Column {@code lac} is the
     * station identifier.
     */
    @Select("SELECT lac AS id, longitude, latitude FROM jizhandata")
    List<Station> findAll();
}
