package org.example.voronoi.db;

import org.apache.ibatis.annotations.Select;
import org.example.voronoi.model.UserRecord;

import java.util.List;

/** Signalling table access. */
public interface UserRecordMapper {

    /** {@code time} is {@code yyyyMMddHHmmss} in the original data set. */
    @Select("SELECT time, imsi, longitude, latitude, lac FROM dempsey")
    List<UserRecord> findAll();
}
