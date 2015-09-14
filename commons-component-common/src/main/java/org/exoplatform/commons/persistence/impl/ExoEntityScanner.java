/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.commons.persistence.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.exoplatform.commons.api.persistence.ExoEntityProcessor;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This integrator is called by Hibernate at run time, when JPA creates the PLF
 * EntityManagerFactory. This integrator reads the eXo JPA entities indexes and
 * registers the entities in the Persistence Unit. The indexes are generated at
 * compile time by ExoEntityProcessor.
 *
 * @see org.exoplatform.commons.api.persistence.ExoEntity
 * @see org.exoplatform.commons.api.persistence.ExoEntityProcessor
 * @author <a href="bdechateauvieux@exoplatform.org">Benoit de Chateauvieux</a>
 * @version $Revision$
 */
public class ExoEntityScanner implements Integrator {
  private static final Log    LOGGER        = ExoLogger.getLogger(ExoEntityScanner.class);
  private static final String PU_FIELD_NAME = "persistenceUnitName";

  public void integrate(Configuration configuration,
                        SessionFactoryImplementor sessionFactory,
                        SessionFactoryServiceRegistry serviceRegistry) {
    try {
      if (isExoPersistenceUnit(configuration)) {
        // get all the exo-entities.idx in classpath
        Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(ExoEntityProcessor.ENTITIES_IDX_PATH);
        while (urls.hasMoreElements()) {
          InputStream stream = urls.nextElement().openStream();
          try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String entityClassName;
            while ((entityClassName = reader.readLine()) != null) {
              try {
                if (configuration.getClassMapping(entityClassName) == null) {
                  configuration.addAnnotatedClass(Class.forName(entityClassName));
                }
              } catch (ClassNotFoundException e) {
                LOGGER.error("Error while trying to register entity [" + entityClassName + "] in Persistence Unit", e);
              }
            }
          } finally {
            try {
              stream.close();
            } catch (IOException e) {
              LOGGER.error("Error while closing stream", e);
            }
          }

        }
      }
    } catch (IOException | IllegalAccessException e) {
      LOGGER.error("Error while loading entities in PLF Persistence Unit", e);
    }
    configuration.buildMappings();
  }

  private boolean isExoPersistenceUnit(Configuration configuration) throws IllegalAccessException {
    SessionFactoryObserver observer = configuration.getSessionFactoryObserver();
    if (observer!=null) {
      for (Field fieldObserver : observer.getClass().getDeclaredFields()) {
        fieldObserver.setAccessible(true);
        Object value = fieldObserver.get(observer);
        for (Field field : value.getClass().getDeclaredFields()) {
          field.setAccessible(true);
          if (PU_FIELD_NAME.equals(field.getName())) {
            return EntityManagerService.PERSISTENCE_UNIT_NAME.equals(field.get(value));
          }
        }
      }
    }
    return false;
  }

  public void integrate(MetadataImplementor metadata,
                        SessionFactoryImplementor sessionFactory,
                        SessionFactoryServiceRegistry serviceRegistry) {
    // Nothing
  }

  public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
    // Nothing
  }

}
