package com.example.vdnh.repo;

import com.example.vdnh.model.Place;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PlaceRepository extends CrudRepository<Place,Long> {

    @Query("SELECT p.latitude, p.longitude, 6371000*SQRT(POW((?1 - p.latitude)*3.14/180,2) +POW( COS((?1 + p.latitude)*3.14/2*180)*(?2-p.longitude)*3.14/180, 2)) AS dist FROM Place p WHERE p.type='Туалеты' ORDER BY dist ASC")
    public List<Object> getToiletsNear(double latitude, double longitude);

    @Query("SELECT p.latitude, p.longitude, 6371000*SQRT(POW((?1 - p.latitude)*3.14/180,2) +POW( COS((?1 + p.latitude)*3.14/2*180)*(?2-p.longitude)*3.14/180, 2)) AS dist FROM Place p WHERE p.type='Остановка' ORDER BY dist ASC")
    public List<Object> getBusStationsNear(double latitude, double longitude);

    public List<Place> findAllByType(String type);

    public List<Place> findAllByLatitudeAfterAndLongitudeAfter(double latitude, double longitude);
}
