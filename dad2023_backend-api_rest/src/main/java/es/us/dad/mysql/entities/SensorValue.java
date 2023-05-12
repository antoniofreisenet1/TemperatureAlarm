package es.us.dad.mysql.entities;

/**
 * This class represents the values generated by a sensor. Every time the value
 * of a certain sensor is reported, a new instance of this entity will be
 * generated in the database. The values in the database are never overwritten,
 * but new ones are generated with their corresponding timestamp.
 * 
 * @author luismi
 *
 */
public class SensorValue {

	/**
	 * Primary key associated with the sensor value. This identifier is unique for
	 * each value of a sensor in the database. In this way, each time a new sensor
	 * value is generated and it is stored in the database, a new tuple will be
	 * generated with a new identifier.
	 */
	private Integer idSensorValue;

	/**
	 * Numerical value obtained by the sensor associated with the indicated sensor
	 * id.
	 */
	private Float value;

	/**
	 * Identifier of the sensor on which said value has been generated. This sensor
	 * uniquely represents an sensor connected to a device.
	 */
	private Integer idSensor;

	/**
	 * Timestamp in which the data provided by the sensor is generated. The
	 * timestamp is expressed in unix time, defined as the milliseconds since
	 * January 1, 1970.
	 */
	private Long timestamp;

	/**
	 * Logical value indicating if the sensor value has been removed. The
	 * elimination is done by marking this field to true, so the value is never
	 * permanently eliminated from the database.
	 */
	private Boolean removed;

	public SensorValue() {
		super();
	}

	public SensorValue(Float value, Integer idSensor, Long timestamp, Boolean removed) {
		super();
		this.value = value;
		this.idSensor = idSensor;
		this.removed = removed;
		this.timestamp = timestamp;
	}

	public SensorValue(Integer idSensorValue, Float value, Integer idSensor, Long timestamp, Boolean removed) {
		super();
		this.idSensorValue = idSensorValue;
		this.value = value;
		this.idSensor = idSensor;
		this.removed = removed;
		this.timestamp = timestamp;
	}

	public Float getValue() {
		return value;
	}

	public void setValue(Float value) {
		this.value = value;
	}

	public Integer getIdSensor() {
		return idSensor;
	}

	public void setIdSensor(Integer idSensor) {
		this.idSensor = idSensor;
	}

	public Boolean isRemoved() {
		return removed;
	}

	public void setRemoved(Boolean removed) {
		this.removed = removed;
	}

	public Integer getIdSensorValue() {
		return idSensorValue;
	}

	public void setIdSensorValue(Integer idSensorValue) {
		this.idSensorValue = idSensorValue;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((idSensor == null) ? 0 : idSensor.hashCode());
		result = prime * result + ((idSensorValue == null) ? 0 : idSensorValue.hashCode());
		result = prime * result + ((removed == null) ? 0 : removed.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SensorValue other = (SensorValue) obj;
		if (idSensor == null) {
			if (other.idSensor != null)
				return false;
		} else if (!idSensor.equals(other.idSensor))
			return false;
		if (idSensorValue == null) {
			if (other.idSensorValue != null)
				return false;
		} else if (!idSensorValue.equals(other.idSensorValue))
			return false;
		if (removed == null) {
			if (other.removed != null)
				return false;
		} else if (!removed.equals(other.removed))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SensorValue [idSensorValue=" + idSensorValue + ", value=" + value + ", idSensor=" + idSensor
				+ ", timestamp=" + timestamp + ", removed=" + removed + "]";
	}

	public boolean equalsWithNoIdConsidered(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SensorValue other = (SensorValue) obj;
		if (idSensor == null) {
			if (other.idSensor != null)
				return false;
		} else if (!idSensor.equals(other.idSensor))
			return false;
		if (removed == null) {
			if (other.removed != null)
				return false;
		} else if (!removed.equals(other.removed))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
