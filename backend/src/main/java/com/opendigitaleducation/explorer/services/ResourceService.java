package com.opendigitaleducation.explorer.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Optional;
import java.util.Set;

//TODO add log profile
//TODO add metrics
public interface ResourceService {

    Future<Void> initMapping(final String application);

    Future<Void> dropMapping(final String application);

    Future<JsonArray> fetch(final UserInfos user, final String application, final ResourceSearchOperation operation);

    Future<FetchResult> fetchWithMeta(final UserInfos user, final String application, final ResourceSearchOperation operation);

    Future<Integer> count(final UserInfos user, final String application, final ResourceSearchOperation operation);

    Future<JsonObject> move(final UserInfos user, final String application, final JsonObject document, final Optional<String> dest);

    Future<JsonArray> trash(UserInfos user, String application, Set<Integer> ids, boolean isTrash);

    Future<JsonObject> move(final UserInfos user, final String application, final Integer id, final Optional<String> dest);

    Future<JsonArray> move(final UserInfos user, final String application, final Set<Integer> id, final Optional<String> dest);

    Future<JsonArray> delete(final UserInfos user, final String application, final String resourceType,final Set<String> id);

    Future<JsonObject> share(final UserInfos user, final String application, final JsonObject document, final List<ShareOperation> operation) throws Exception;

    Future<List<JsonObject>> share(final UserInfos user, final String application, final List<JsonObject> documents, final List<ShareOperation> operation) throws Exception;

    void stopConsumer();

    class FetchResult{
        public final Long count;
        public final List<JsonObject> rows;

        public FetchResult(Long count, List<JsonObject> rows) {
            this.count = count;
            this.rows = rows;
        }
    }

    class ShareOperation {
        final String id;
        final boolean group;
        final JsonObject rights;

        public ShareOperation(String id, boolean group, JsonObject rights) {
            this.id = id;
            this.group = group;
            this.rights = rights;
        }

        public String getId() {
            return id;
        }

        public boolean isGroup() {
            return group;
        }

        public JsonObject getRights() {
            return rights;
        }

        public JsonObject toJsonRight() {
            return this.rights.copy().put(group ? "groupId" : "userId", id);
        }
    }

}
