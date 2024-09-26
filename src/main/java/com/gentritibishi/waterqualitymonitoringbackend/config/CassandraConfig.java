package com.gentritibishi.waterqualitymonitoringbackend.config;

import com.gentritibishi.waterqualitymonitoringbackend.helpers.InstantToLongConverter;
import com.gentritibishi.waterqualitymonitoringbackend.helpers.LongToInstantConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;

import java.util.Arrays;

@Configuration
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Bean
    @Override
    public CassandraCustomConversions customConversions() {
        return new CassandraCustomConversions(Arrays.asList(new InstantToLongConverter(), new LongToInstantConverter()));
    }

    @Override
    protected String getKeyspaceName() {
        return "sensor_data"; // Replace with your keyspace name
    }

    @Override
    protected String getContactPoints() {
        return "127.0.0.1"; // Replace with your contact point(s)
    }

    @Override
    protected int getPort() {
        return 9042; // Replace with your Cassandra port
    }

    @Override
    protected String getLocalDataCenter() {
        return "datacenter1"; // Replace with your data center name
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }


}

