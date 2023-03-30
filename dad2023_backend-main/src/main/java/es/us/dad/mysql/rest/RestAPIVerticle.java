package es.us.dad.mysql.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.rowset.internal.Row;

import es.us.dad.mysql.entities.Device;
import es.us.dad.mysql.entities.Group;
import es.us.dad.mysql.entities.Sensor;
import es.us.dad.mysql.entities.SensorValue;
import es.us.dad.mysql.messages.DatabaseEntity;
import es.us.dad.mysql.messages.DatabaseMessage;
import es.us.dad.mysql.messages.DatabaseMessageLatestValues;
import es.us.dad.mysql.messages.DatabaseMessageType;
import es.us.dad.mysql.messages.DatabaseMethod;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class RestAPIVerticle extends AbstractVerticle {

	private transient Gson gson;

	@Override
	public void start(Promise<Void> startFuture) {

		// Instantiating a Gson serialize object using specific date format
		gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

		// Defining the router object
		Router router = Router.router(vertx);

		// Handling any server startup result
		HttpServer httpServer = vertx.createHttpServer();
		httpServer.requestHandler(router::handle).listen(80, result -> {
			if (result.succeeded()) {
				System.out.println("API Rest is listening on port 80");
				startFuture.complete();
			} else {
				startFuture.fail(result.cause());
			}
		});

		// Defining URI paths for each method in RESTful interface, including body
		// handling
		router.route("/api*").handler(BodyHandler.create());

		// Endpoint definition for CRUD ops
		router.get("/api/sensors/:sensorid").handler(this::getSensorById);
		router.post("/api/sensors").handler(this::addSensor);
		router.delete("/api/sensors/:sensorid").handler(this::deleteSensor);
		router.put("/api/sensors/:sensorid").handler(this::putSensor);
		
		
		
		//Group endpoints:
		
		router.get("/api/groups/:groupid").handler(this::getGroupById);
		router.post("/api/groups").handler(this::addGroup);
		router.delete("/api/groups/:groupid").handler(this::deleteGroup);
		router.put("/api/groups/:groupid").handler(this::putGroup);
		router.put("/api/groups/:groupid/:deviceid").handler(this::addDeviceToGroup);
		router.get("/api/groups/:groupid").handler(this::getDevicesFromGroupId);
		
		//SensorValue endpoints:
		
		router.get("/api/sensorvalues/:sensorid/:limit").handler(this::getLatestSensorValueBySensorId);
		router.post("/api/sensorvalues").handler(this::addSensorValue);
		router.get("/api/sensorvalues/:sensorid").handler(this::getLastSensorValueBySensorId);
		router.delete("/api/sensorvalues/:sensorvalueid").handler(this::deleteSensorValue);
	}

	/**
	 * Deserialization of the message sent in the body of a message to the
	 * DatabaseMessage type. It is useful for managing the exchange of messages
	 * between the controller and the Rest API.
	 * 
	 * @param handler AsyncResult<Message<Object>> returned by controller Verticle
	 * 
	 * @return DatabaseMessage deserialized
	 */
	private DatabaseMessage deserializeDatabaseMessageFromMessageHandler(AsyncResult<Message<Object>> handler) {
		return gson.fromJson(handler.result().body().toString(), DatabaseMessage.class);
	}

	/**
	 * GET Sensor handler function for /api/sensors/:sensorid endpoint
	 * 
	 * @param routingContext
	 */
	private void getSensorById(RoutingContext routingContext) {
		int sensorId = Integer.parseInt(routingContext.request().getParam("sensorid"));

		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Sensor,
				DatabaseMethod.GetSensor, sensorId);

		vertx.eventBus().request(RestEntityMessage.Sensor.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Sensor.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}

	/**
	 * POST Sensor handler function for /api/sensors endpoint
	 * 
	 * @param routingContext
	 */
	private void addSensor(RoutingContext routingContext) {
		final Sensor sensor = gson.fromJson(routingContext.getBodyAsString(), Sensor.class);
		if (sensor == null || sensor.getIdDevice() == null || sensor.getSensorType() == null) {
			routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			return;
		}
		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Sensor,
				DatabaseMethod.CreateSensor, gson.toJson(sensor));

		vertx.eventBus().request(RestEntityMessage.Sensor.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(201)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Sensor.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}

	/**
	 * DELETE Sensor handler function for /api/sensors/:sensorid endpoint
	 * 
	 * @param routingContext
	 */
	private void deleteSensor(RoutingContext routingContext) {
		int sensorId = Integer.parseInt(routingContext.request().getParam("sensorid"));

		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.Sensor,
				DatabaseMethod.DeleteSensor, sensorId);

		vertx.eventBus().request(RestEntityMessage.Sensor.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(responseMessage.getResponseBody());
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}

	/**
	 * PUT Sensor handler function for /api/sensors/:sensorid endpoint
	 * 
	 * @param routingContext
	 */
	private void putSensor(RoutingContext routingContext) {
		final Sensor sensor = gson.fromJson(routingContext.getBodyAsString(), Sensor.class);
		int sensorId = Integer.parseInt(routingContext.request().getParam("sensorid"));

		if (sensor == null) {
			routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			return;
		}

		sensor.setIdSensor(sensorId);
		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Sensor,
				DatabaseMethod.EditSensor, gson.toJson(sensor));

		vertx.eventBus().request(RestEntityMessage.Sensor.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(201)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Sensor.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}
	
	private void getGroupById(RoutingContext routingContext) {
		int groupId = Integer.parseInt(routingContext.request().getParam("groupid"));

		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Group,
				DatabaseMethod.GetGroup, groupId);

		vertx.eventBus().request(RestEntityMessage.Group.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Group.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}

	
	private void addGroup(RoutingContext routingContext) {
		final Group group = gson.fromJson(routingContext.getBodyAsString(), Group.class);
		if (group == null || group.getIdGroup() == null || group.getName() == null || group.getMqttChannel() == null) {
			routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			return;
		}
		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Group,
				DatabaseMethod.CreateGroup, gson.toJson(group));

		vertx.eventBus().request(RestEntityMessage.Group.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(201)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Group.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}
	
	private void addDeviceToGroup(RoutingContext routingContext) {
		final Device device = new Device();
		Integer groupId = Integer.parseInt(routingContext.request().getParam("groupId"));
		Integer deviceId =Integer.parseInt(routingContext.request().getParam("deviceId"));
		
		if (groupId == null || deviceId == null) {
			routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			return;
		}
		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Group,
				DatabaseMethod.AddDeviceToGroup, gson.toJson(device));

		vertx.eventBus().request(RestEntityMessage.Group.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(201)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Group.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}
	
	private void getDevicesFromGroupId(RoutingContext routingContext) {
		int groupId = Integer.parseInt(routingContext.request().getParam("groupid"));

		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Group,
				DatabaseMethod.GetDevicesFromGroupId, groupId);

		vertx.eventBus().request(RestEntityMessage.Group.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Group.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}
	

	private void deleteGroup(RoutingContext routingContext) {
		int groupId = Integer.parseInt(routingContext.request().getParam("groupid"));

		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.Group,
				DatabaseMethod.DeleteGroup, groupId);

		vertx.eventBus().request(RestEntityMessage.Group.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(responseMessage.getResponseBody());
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}

	private void putGroup(RoutingContext routingContext) {
		final Group group = gson.fromJson(routingContext.getBodyAsString(), Group.class);
		int groupId = Integer.parseInt(routingContext.request().getParam("groupid"));

		if (group == null) {
			routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			return;
		}

		group.setIdGroup(groupId);
		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Group,
				DatabaseMethod.EditGroup, gson.toJson(group));

		vertx.eventBus().request(RestEntityMessage.Group.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(201)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Group.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}
	
	private void getLatestSensorValueBySensorId(RoutingContext routingContext) {
		int sensorVId = Integer.parseInt(routingContext.request().getParam("sensorvalueid"));
		int limit = Integer.parseInt(routingContext.request().getParam("limit"));
		DatabaseMessageLatestValues i = new DatabaseMessageLatestValues(sensorVId, limit);

		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.SensorValue,
				DatabaseMethod.GetLatestSensorValuesFromSensorId, i);

		vertx.eventBus().request(RestEntityMessage.SensorValue.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(gson.toJson(responseMessage.getResponseBodyAs(Sensor.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}
	
	private void getLastSensorValueBySensorId(RoutingContext routingContext) {
		int sensorId = Integer.parseInt(routingContext.request().getParam("sensorid"));

		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.SensorValue,
				DatabaseMethod.GetLastSensorValueFromSensorId, sensorId);

		vertx.eventBus().request(RestEntityMessage.SensorValue.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(gson.toJson(responseMessage.getResponseBodyAs(SensorValue.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}
	
	private void addSensorValue(RoutingContext routingContext) {
		final SensorValue sensorValue = gson.fromJson(routingContext.getBodyAsString(), SensorValue.class);
		if (sensorValue == null || sensorValue.getIdSensorValue() == null) {
			routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			return;
		}
		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.SensorValue,
				DatabaseMethod.CreateSensorValue, gson.toJson(sensorValue));

		vertx.eventBus().request(RestEntityMessage.SensorValue.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(201)
						.end(gson.toJson(responseMessage.getResponseBodyAs(SensorValue.class)));
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}
	
	private void deleteSensorValue(RoutingContext routingContext) {
		int sensorValueId = Integer.parseInt(routingContext.request().getParam("sensorValueid"));

		DatabaseMessage databaseMessage = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.SensorValue,
				DatabaseMethod.DeleteSensorValue, sensorValueId);

		vertx.eventBus().request(RestEntityMessage.SensorValue.getAddress(), gson.toJson(databaseMessage), handler -> {
			if (handler.succeeded()) {
				DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(responseMessage.getResponseBody());
			} else {
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(500).end();
			}
		});
	}



	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		super.stop(stopFuture);
	}

}
