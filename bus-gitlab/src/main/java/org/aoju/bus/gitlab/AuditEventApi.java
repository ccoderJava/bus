/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org Greg Messner and other contributors.         *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.gitlab;

import org.aoju.bus.gitlab.models.AuditEvent;
import org.aoju.bus.gitlab.support.ISO8601;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class implements the client side API for the GitLab Instance Audit Event API.
 * See <a href="https://docs.gitlab.com/ee/api/audit_events.html">Audit Event API at GitLab</a> for more information.
 */
public class AuditEventApi extends AbstractApi {

    public AuditEventApi(GitLabApi gitLabApi) {
        super(gitLabApi);
    }

    /**
     * Get a List of the group audit events viewable by Maintainer or an Owner of the group.
     *
     * <pre><code>GET /audit_events/</code></pre>
     *
     * @param created_after  Return group audit events created on or after the given time.
     * @param created_before Return group audit events created on or before the given time.
     * @param entityType     Return audit events for the given entity type. Valid values are: User, Group, or Project.
     * @param entityId       Return audit events for the given entity ID. Requires entityType attribute to be present.
     * @return a List of group Audit events
     * @throws GitLabApiException if any exception occurs
     */
    public List<AuditEvent> getAuditEvents(Date created_after, Date created_before, String entityType, Integer entityId) throws GitLabApiException {
        return (getAuditEvents(created_after, created_before, entityType, entityId, getDefaultPerPage()).all());
    }

    /**
     * Get a Pager of the group audit events viewable by Maintainer or an Owner of the group.
     *
     * <pre><code>GET /audit_events</code></pre>
     *
     * @param created_after  Return group audit events created on or after the given time.
     * @param created_before Return group audit events created on or before the given time.
     * @param entityType     Return audit events for the given entity type. Valid values are: User, Group, or Project.
     * @param entityId       Return audit events for the given entity ID. Requires entityType attribute to be present.
     * @param itemsPerPage   the number of Audit Event instances that will be fetched per page
     * @return a Pager of group Audit events
     * @throws GitLabApiException if any exception occurs
     */
    public Pager<AuditEvent> getAuditEvents(Date created_after, Date created_before, String entityType, Integer entityId, int itemsPerPage) throws GitLabApiException {
        Form form = new GitLabApiForm()
                .withParam("created_before", ISO8601.toString(created_before, false))
                .withParam("created_after", ISO8601.toString(created_after, false))
                .withParam("entity_type", entityType)
                .withParam("entity_id", entityId);
        return (new Pager<AuditEvent>(this, AuditEvent.class, itemsPerPage, form.asMap(), "audit_events"));
    }

    /**
     * Get a Stream of the group audit events viewable by Maintainer or an Owner of the group.
     *
     * <pre><code>GET /audit_events</code></pre>
     *
     * @param created_after  Return group audit events created on or after the given time.
     * @param created_before Return group audit events created on or before the given time.
     * @param entityType     Return audit events for the given entity type. Valid values are: User, Group, or Project.
     * @param entityId       Return audit events for the given entity ID. Requires entityType attribute to be present.
     * @return a Stream of group Audit events
     * @throws GitLabApiException if any exception occurs
     */
    public Stream<AuditEvent> getAuditEventsStream(Date created_after, Date created_before, String entityType, Integer entityId) throws GitLabApiException {
        return (getAuditEvents(created_after, created_before, entityType, entityId, getDefaultPerPage()).stream());
    }

    /**
     * Get a specific instance audit event.
     *
     * <pre><code>GitLab Endpoint: GET /audit_events/:id</code></pre>
     *
     * @param auditEventId the auditEventId, required
     * @return the group Audit event
     * @throws GitLabApiException if any exception occurs
     */
    public AuditEvent getAuditEvent(Integer auditEventId) throws GitLabApiException {
        Response response = get(Response.Status.OK, null, "audit_events", auditEventId);
        return (response.readEntity(AuditEvent.class));
    }
}
