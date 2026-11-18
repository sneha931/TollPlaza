package com.freightfox.tollplaza.domain;

import com.freightfox.tollplaza.geo.GeoPoint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A toll plaza from the reference CSV.
 * <p>
 * (name, latitude, longitude) is the natural key: the CSV repeats rows, and a few different
 * plazas share a name, so neither the name nor the coordinates alone are unique.
 */
@Entity
@Table(name = "toll_plaza",
		uniqueConstraints = @UniqueConstraint(name = "uk_toll_plaza_natural_key",
				columnNames = { "name", "latitude", "longitude" }),
		indexes = @Index(name = "idx_toll_plaza_lat_lon", columnList = "latitude, longitude"))
public class TollPlaza {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private double longitude;

	private String state;

	protected TollPlaza() {
		// for JPA
	}

	public TollPlaza(String name, double latitude, double longitude, String state) {
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.state = state;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getState() {
		return state;
	}

	public GeoPoint location() {
		return new GeoPoint(latitude, longitude);
	}
}
