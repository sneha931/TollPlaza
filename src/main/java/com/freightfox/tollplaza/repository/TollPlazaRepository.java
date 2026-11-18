package com.freightfox.tollplaza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freightfox.tollplaza.domain.TollPlaza;

public interface TollPlazaRepository extends JpaRepository<TollPlaza, Long> {

	/** Index-backed bounding-box lookup used to shortlist plazas near a route. */
	List<TollPlaza> findByLatitudeBetweenAndLongitudeBetween(double minLat, double maxLat, double minLon,
			double maxLon);
}
