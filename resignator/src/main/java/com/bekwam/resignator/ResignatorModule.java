package com.bekwam.resignator;

import com.bekwam.jfxbop.data.ManagedDataSource;
import com.bekwam.jfxbop.data.ManagedDataSourceInterceptor;
import com.bekwam.resignator.model.ConfigurationDataSource;
import com.bekwam.resignator.model.ConfigurationDataSourceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.BuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by carl_000 on 6/29/2015.
 */
public class ResignatorModule extends AbstractModule {

    private final static Logger logger = LoggerFactory.getLogger(ResignatorModule.class);

    private final static String CONFIG_DIR = ".resignator";
    private final static String CONFIG_FILE = "resignator.json";
    private final static Integer NUM_RECENT_PROFILES = 4;

    @Override
    protected void configure() {

        if( logger.isDebugEnabled() ) {
            logger.debug("[CONFIGURE] configuring");
        }

        bind(BuilderFactory.class).to(JavaFXBuilderFactory.class);

        bindInterceptor(Matchers.subclassesOf(ManagedDataSource.class), Matchers.any(), new ManagedDataSourceInterceptor());

        bind(String.class).annotatedWith(Names.named("ConfigDir")).toInstance(CONFIG_DIR);
        bind(String.class).annotatedWith(Names.named("ConfigFile")).toInstance(CONFIG_FILE);
        bind(Integer.class).annotatedWith(Names.named("NumRecentProfiles")).toInstance(NUM_RECENT_PROFILES);

        bind(ConfigurationDataSource.class).to(ConfigurationDataSourceImpl.class);
    }
}
