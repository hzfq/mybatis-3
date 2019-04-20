/**
 * Copyright 2009-2019 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.submitted.cursor_simple;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.util.SocketUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("EmbeddedPostgresqlTests")
class PostgresCursorTest {

    private static final EmbeddedPostgres postgres = new EmbeddedPostgres();

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void setUp() throws Exception {
        // Launch PostgreSQL server. Download / unarchive if necessary.
        String url = postgres.start(
                EmbeddedPostgres.cachedRuntimeConfig(Paths.get(System.getProperty("java.io.tmpdir"), "pgembed")), "localhost",
                SocketUtil.findFreePort(), "cursor_simple", "postgres", "root", Collections.emptyList());

        Configuration configuration = new Configuration();
        Environment environment = new Environment("development", new JdbcTransactionFactory(), new UnpooledDataSource(
                "org.postgresql.Driver", url, null));
        configuration.setEnvironment(environment);
        configuration.addMapper(Mapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "org/apache/ibatis/submitted/cursor_simple/CreateDB.sql");
    }

    @AfterAll
    static void tearDown() {
        postgres.stop();
    }

    @Test
    void shouldBeAbleToReuseStatement() throws IOException {
        // #1351
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.REUSE)) {
            Mapper mapper = sqlSession.getMapper(Mapper.class);
            {
                Cursor<User> usersCursor = mapper.getAllUsers();
                Iterator<User> iterator = usersCursor.iterator();
                User user = iterator.next();
                assertEquals("User1", user.getName());
                usersCursor.close();
            }
            {
                Cursor<User> usersCursor = mapper.getAllUsers();
                Iterator<User> iterator = usersCursor.iterator();
                User user = iterator.next();
                assertEquals("User1", user.getName());
                usersCursor.close();
            }
        }
    }
}
