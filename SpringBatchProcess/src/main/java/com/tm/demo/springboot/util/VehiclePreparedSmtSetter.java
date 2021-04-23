package com.tm.demo.springboot.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import org.springframework.batch.item.database.ItemPreparedStatementSetter;

import com.tm.demo.springboot.model.Vehicle;

public class VehiclePreparedSmtSetter implements ItemPreparedStatementSetter<Vehicle> {
    @Override
    public void setValues(Vehicle Vehicle, 
                          PreparedStatement preparedStatement) throws SQLException {
    	Random r = new Random();
    	preparedStatement.setLong(1,r.nextLong());
        preparedStatement.setString(2, Vehicle.getVehiclenumber());
        preparedStatement.setString(3, Vehicle.getBrand());
        preparedStatement.setString(4, Vehicle.getCountry());
        preparedStatement.setString(5, Vehicle.getModelname());
        preparedStatement.setString(6,  Vehicle.getModelyear());
    }
}
