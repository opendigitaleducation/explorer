package com.opendigitaleducation.explorer;

import com.opendigitaleducation.explorer.ingest.IngestJob;
import com.opendigitaleducation.explorer.ingest.MessageReader;
import com.opendigitaleducation.explorer.services.ResourceService;
import com.opendigitaleducation.explorer.services.ResourceSearchOperation;
import com.opendigitaleducation.explorer.services.impl.ResourceServiceElastic;
import com.opendigitaleducation.explorer.share.DefaultShareTableManager;
import com.opendigitaleducation.explorer.share.ShareTableManager;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.elasticsearch.ElasticClientManager;
import org.entcore.common.explorer.IExplorerPluginClient;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.impl.ExplorerPluginClient;
import org.entcore.common.explorer.impl.ExplorerPluginCommunicationPostgres;
import org.entcore.common.postgres.PostgresClient;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RunWith(VertxUnitRunner.class)
public class MongoPluginTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static ElasticsearchContainer esContainer = test.database().createOpenSearchContainer().withReuse(true);
    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer().withInitScript("initExplorer.sql").withReuse(true);
    @ClassRule
    public static MongoDBContainer mongoDBContainer = test.database().createMongoContainer().withReuse(true);

    static ElasticClientManager elasticClientManager;
    static ResourceService resourceService;
    static FakeMongoPlugin plugin;
    static String application;
    static IngestJob job;
    static MongoClient mongoClient;
    static ExplorerPluginClient pluginClient;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        final URI[] uris = new URI[]{new URI("http://" + esContainer.getHttpHostAddress())};
        elasticClientManager = new ElasticClientManager(test.vertx(), uris);
        final String resourceIndex = ExplorerConfig.DEFAULT_RESOURCE_INDEX + "_" + System.currentTimeMillis();
        System.out.println("Using index: " + resourceIndex);
        ExplorerConfig.getInstance().setEsIndex(FakeMongoPlugin.FAKE_APPLICATION, resourceIndex);
        final JsonObject mongoConfig = new JsonObject().put("connection_string", mongoDBContainer.getReplicaSetUrl());
        final JsonObject postgresqlConfig = new JsonObject().put("host", pgContainer.getHost()).put("database", pgContainer.getDatabaseName()).put("user", pgContainer.getUsername()).put("password", pgContainer.getPassword()).put("port", pgContainer.getMappedPort(5432));
        final PostgresClient postgresClient = new PostgresClient(test.vertx(), postgresqlConfig);
        final ShareTableManager shareTableManager = new DefaultShareTableManager();
        IExplorerPluginCommunication communication = new ExplorerPluginCommunicationPostgres(test.vertx(), postgresClient);
        mongoClient = MongoClient.createShared(test.vertx(), mongoConfig);
        resourceService = new ResourceServiceElastic(elasticClientManager, shareTableManager, communication, postgresClient);
        plugin = FakeMongoPlugin.withPostgresChannel(test.vertx(), postgresClient, mongoClient);
        application = plugin.getApplication();
        final Async async = context.async();
        createMapping(elasticClientManager, context, resourceIndex).onComplete(r -> async.complete());
        final MessageReader reader = MessageReader.postgres(postgresClient, new JsonObject());
        job = IngestJob.create(test.vertx(), elasticClientManager, postgresClient, new JsonObject(), reader);
        pluginClient = IExplorerPluginClient.withBus(test.vertx(), FakeMongoPlugin.FAKE_APPLICATION, FakeMongoPlugin.FAKE_TYPE);
        final JsonObject rights = new JsonObject();
        rights.put(ExplorerConfig.RIGHT_READ, ExplorerConfig.RIGHT_READ);
        rights.put(ExplorerConfig.RIGHT_CONTRIB, ExplorerConfig.RIGHT_CONTRIB);
        rights.put(ExplorerConfig.RIGHT_MANAGE, ExplorerConfig.RIGHT_MANAGE);
        ExplorerConfig.getInstance().addRightsForApplication(FakeMongoPlugin.FAKE_APPLICATION, rights);
    }


    static Future<Void> createMapping(ElasticClientManager elasticClientManager, TestContext context, String index) {
        final Buffer mapping = test.vertx().fileSystem().readFileBlocking("es/mappingResource.json");
        return elasticClientManager.getClient().createMapping(index, mapping);
    }

    static JsonObject resource(final String name) {
        return new JsonObject().put("name", name);
    }

    @Test
    public void shouldCreateResource(TestContext context) {
        final JsonObject f1 = resource("folder1");
        final JsonObject f2 = resource("folder2");
        final JsonObject f3 = resource("folder3");
        final UserInfos user = test.directory().generateUser("usermove");
        final Async async = context.async();
        resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch0 -> {
            context.assertEquals(0, fetch0.size());
            plugin.create(user, Arrays.asList(f1, f2, f3), false).onComplete(context.asyncAssertSuccess(r -> {
                job.execute(true).onComplete(context.asyncAssertSuccess(r4 -> {
                    resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch1 -> {
                        context.assertEquals(3, fetch1.size());
                        async.complete();
                    }));
                }));
            }));
        }));
    }


    @Test
    public void shouldReindexResource(TestContext context) {
        final UserInfos user = test.directory().generateUser("reindex");
        final JsonObject f1 = resource("reindex1").put("creatorId", user.getUserId()).put("id", "reindex1");
        final JsonObject f2 = resource("reindex2").put("creatorId", user.getUserId()).put("id", "reindex2");
        final JsonObject f3 = resource("reindex3").put("creatorId", user.getUserId()).put("id", "reindex3");
        final Promise p1 = Promise.promise();
        final Promise p2 = Promise.promise();
        final Promise p3 = Promise.promise();
        mongoClient.insert(FakeMongoPlugin.COLLECTION, f1, p1);
        mongoClient.insert(FakeMongoPlugin.COLLECTION, f2, p2);
        mongoClient.insert(FakeMongoPlugin.COLLECTION, f3, p3);
        final Async async = context.async();
        plugin.start();
        CompositeFuture.all(p1.future(), p2.future(), p3.future()).onComplete(context.asyncAssertSuccess(r1 -> {
            job.execute(true).onComplete(context.asyncAssertSuccess(r0 -> {
                resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch0 -> {
                    context.assertEquals(0, fetch0.size());
                    pluginClient.getForIndexation(user, Optional.empty(), Optional.empty()).onComplete(context.asyncAssertSuccess(r2 -> {
                        plugin.getCommunication().waitPending().onComplete(context.asyncAssertSuccess(r3 -> {
                            job.execute(true).onComplete(context.asyncAssertSuccess(r4 -> {
                                job.waitPending().onComplete(context.asyncAssertSuccess(r5 -> {
                                    resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch1 -> {
                                        context.assertEquals(3, fetch1.size());
                                        async.complete();
                                    }));
                                }));
                            }));
                        }));
                    }));
                }));
            }));
        }));
    }

    @Test
    public void shouldMoveIfOwner(TestContext context) {
        final UserInfos user = test.directory().generateUser("user_move1");
        final UserInfos user2 = test.directory().generateUser("user_move2");
        final JsonObject f1 = resource("reindex1").put("creatorId", user.getUserId()).put("id", "reindex1");
        final Async async = context.async(2);
        plugin.create(user, Arrays.asList(f1), false).onComplete(context.asyncAssertSuccess(r -> {
            job.execute(true).onComplete(context.asyncAssertSuccess(r4a -> {
                resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch -> {
                    context.assertEquals(1, fetch.size());
                    final Integer fId = Integer.valueOf(fetch.getJsonObject(0).getValue("_id").toString());
                    resourceService.move(user2, application, fId, Optional.empty()).onComplete(context.asyncAssertFailure(move -> {
                        context.assertEquals(move.getMessage(), "resource.move.id.invalid");
                        async.countDown();
                    }));
                    resourceService.move(user, application, fId, Optional.empty()).onComplete(context.asyncAssertSuccess(move -> {
                        context.assertEquals(move.getValue("_id").toString(), fId.toString());
                        async.countDown();
                    }));
                }));
            }));
        }));
    }

    @Test
    public void shouldTrashIfOwner(TestContext context) {
        final UserInfos user = test.directory().generateUser("user_trash1");
        final UserInfos user2 = test.directory().generateUser("user_trash2");
        final JsonObject f1 = resource("reindex1").put("creatorId", user.getUserId()).put("id", "reindex1");
        final Async async = context.async(2);
        plugin.create(user, Arrays.asList(f1), false).onComplete(context.asyncAssertSuccess(r -> {
            job.execute(true).onComplete(context.asyncAssertSuccess(r4a -> {
                resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch -> {
                    context.assertEquals(1, fetch.size());
                    final Integer fId = Integer.valueOf(fetch.getJsonObject(0).getValue("_id").toString());
                    final Set<Integer> ids = new HashSet<>();
                    ids.add(fId);
                    resourceService.trash(user2, application, ids, true).onComplete(context.asyncAssertFailure(move -> {
                        context.assertEquals(move.getMessage(), "resource.trash.id.invalid");
                        async.countDown();
                    }));
                    resourceService.trash(user, application, ids, true).onComplete(context.asyncAssertSuccess(move -> {
                        context.assertEquals(move.getJsonObject(0).getValue("_id").toString(), fId.toString());
                        async.countDown();
                    }));
                }));
            }));
        }));
    }

    @Test
    public void shouldDeleteIfOwner(TestContext context) {
        final UserInfos user = test.directory().generateUser("user_del1");
        final UserInfos user2 = test.directory().generateUser("user_del2");
        final JsonObject f1 = resource("reindex1").put("creatorId", user.getUserId()).put("id", "reindex1");
        final Async async = context.async(2);
        plugin.start();
        plugin.create(user, Arrays.asList(f1), false).onComplete(context.asyncAssertSuccess(r -> {
            job.execute(true).onComplete(context.asyncAssertSuccess(r4a -> {
                resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch -> {
                    context.assertEquals(1, fetch.size());
                    final String fId = (fetch.getJsonObject(0).getValue("_id").toString());
                    final Set<String> ids = new HashSet<>();
                    ids.add(fId);
                    resourceService.delete(user2, application, plugin.getResourceType(), ids).onComplete(context.asyncAssertFailure(move -> {
                        context.assertEquals(move.getMessage(), "resource.delete.id.invalid");
                        async.countDown();
                    }));
                    resourceService.delete(user, application, plugin.getResourceType(), ids).onComplete(context.asyncAssertSuccess(move -> {
                        context.assertEquals(move.getJsonObject(0).getValue("_id").toString(), fId);
                        async.countDown();
                    }));
                }));
            }));
        }));
    }

    @Test
    public void shouldMoveIfReadRight(TestContext context) {
        final UserInfos user = test.directory().generateUser("user_move1_read");
        final UserInfos user2 = test.directory().generateUser("user_move2_read");
        final UserInfos user3 = test.directory().generateUser("user_move3_read");
        final JsonObject f1 = resource("reindex1").put("creatorId", user.getUserId()).put("id", "reindex1");
        final JsonObject share = new JsonObject().put("userId",user3.getUserId()).put(ExplorerConfig.RIGHT_READ, true);
        f1.put("shared", new JsonArray().add(share));
        final Async async = context.async(2);
        plugin.create(user, Arrays.asList(f1), false).onComplete(context.asyncAssertSuccess(r -> {
            job.execute(true).onComplete(context.asyncAssertSuccess(r4a -> {
                resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch -> {
                    context.assertEquals(1, fetch.size());
                    final Integer fId = Integer.valueOf(fetch.getJsonObject(0).getValue("_id").toString());
                    resourceService.move(user2, application, fId, Optional.empty()).onComplete(context.asyncAssertFailure(move -> {
                        context.assertEquals(move.getMessage(), "resource.move.id.invalid");
                        async.countDown();
                    }));
                    resourceService.move(user3, application, fId, Optional.empty()).onComplete(context.asyncAssertSuccess(move -> {
                        context.assertEquals(move.getValue("_id").toString(), fId.toString());
                        async.countDown();
                    }));
                }));
            }));
        }));
    }

    @Test
    public void shouldTrashIfManageRight(TestContext context) {
        final UserInfos user = test.directory().generateUser("user_trash1_manage");
        final UserInfos user2 = test.directory().generateUser("user_trash2_manage");
        final UserInfos user3 = test.directory().generateUser("user_trash3_manage");
        final JsonObject f1 = resource("reindex1").put("creatorId", user.getUserId()).put("id", "reindex1");
        final JsonObject share = new JsonObject().put("userId",user3.getUserId()).put(ExplorerConfig.RIGHT_MANAGE, true);
        f1.put("shared", new JsonArray().add(share));
        final Async async = context.async(2);
        plugin.create(user, Arrays.asList(f1), false).onComplete(context.asyncAssertSuccess(r -> {
            job.execute(true).onComplete(context.asyncAssertSuccess(r4a -> {
                resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch -> {
                    context.assertEquals(1, fetch.size());
                    final Integer fId = Integer.valueOf(fetch.getJsonObject(0).getValue("_id").toString());
                    final Set<Integer> ids = new HashSet<>();
                    ids.add(fId);
                    resourceService.trash(user2, application, ids, true).onComplete(context.asyncAssertFailure(move -> {
                        context.assertEquals(move.getMessage(), "resource.trash.id.invalid");
                        async.countDown();
                    }));
                    resourceService.trash(user3, application, ids, true).onComplete(context.asyncAssertSuccess(move -> {
                        context.assertEquals(move.getJsonObject(0).getValue("_id").toString(), fId.toString());
                        async.countDown();
                    }));
                }));
            }));
        }));
    }

    @Test
    public void shouldDeleteIfManageRight(TestContext context) {
        final UserInfos user = test.directory().generateUser("user_del1_manage");
        final UserInfos user2 = test.directory().generateUser("user_del2_manage");
        final UserInfos user3 = test.directory().generateUser("user_del3_manage");
        final JsonObject f1 = resource("reindex1").put("creatorId", user.getUserId()).put("id", "reindex1");
        final JsonObject share = new JsonObject().put("userId",user3.getUserId()).put(ExplorerConfig.RIGHT_MANAGE, true);
        f1.put("shared", new JsonArray().add(share));
        final Async async = context.async(2);
        plugin.start();
        plugin.create(user, Arrays.asList(f1), false).onComplete(context.asyncAssertSuccess(r -> {
            job.execute(true).onComplete(context.asyncAssertSuccess(r4a -> {
                resourceService.fetch(user, application, new ResourceSearchOperation()).onComplete(context.asyncAssertSuccess(fetch -> {
                    context.assertEquals(1, fetch.size());
                    final String fId = (fetch.getJsonObject(0).getValue("_id").toString());
                    final Set<String> ids = new HashSet<>();
                    ids.add(fId);
                    resourceService.delete(user2, application, plugin.getResourceType(), ids).onComplete(context.asyncAssertFailure(move -> {
                        context.assertEquals(move.getMessage(), "resource.delete.id.invalid");
                        async.countDown();
                    }));
                    resourceService.delete(user3, application, plugin.getResourceType(), ids).onComplete(context.asyncAssertSuccess(move -> {
                        context.assertEquals(move.getJsonObject(0).getValue("_id").toString(), fId);
                        async.countDown();
                    }));
                }));
            }));
        }));
    }

}
