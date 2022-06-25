/********************************************************************************
 * Copyright (c) 2022 Imixs Software Solutions GmbH,
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You can receive a copy of the GNU General Public
 * License at http://www.gnu.org/licenses/gpl.html
 *
 * Project:
 *     https://github.com/imixs/open-bpmn
 *
 * Contributors:
 *     Imixs Software Solutions GmbH - Project Management
 *     Ralph Soika - Software Developer
 ********************************************************************************/
package org.openbpmn.glsp.jsonforms;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.logging.Logger;

import org.eclipse.glsp.server.model.DefaultGModelState;
import org.junit.jupiter.api.Test;

/**
 * Test class to test the UISchemaBuilder used to generate JSONForms uiSchematas
 *
 * @author rsoika
 *
 */
public class TestDataBuilder extends DefaultGModelState {

    private static Logger logger = Logger.getLogger(TestDataBuilder.class.getName());

    @Test
    public void testSimple() {

        DataBuilder builder = new DataBuilder(). //
                addData("name", "John"). //
                addData("description", "Hello World");

        String json = builder.build();

        assertNotNull(json);

        logger.info(json);

    }

    @Test
    public void testArray() {

        DataBuilder builder = new DataBuilder(). //
                addData("name", "John"). //
                addData("description", "Hello World");

        builder.addArray("comments");

        // add first object
        builder.addObject();
        builder.addData("language", "javascript");
        builder.addData("script", "1+1");

        // add second object
        builder.addObject();
        builder.addData("language", "java");
        builder.addData("script", "2+2");

        String json = builder.build();

        assertNotNull(json);

        logger.info(json);

    }

}
