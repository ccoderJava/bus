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

import org.aoju.bus.gitlab.models.Note;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class NotesApi extends AbstractApi {

    public NotesApi(GitLabApi gitLabApi) {
        super(gitLabApi);
    }

    /**
     * Get a list of the issues's notes.
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/issues/:issue_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param issueIid        the issue ID to get the notes for
     * @return a list of the issues's notes
     * @throws GitLabApiException if any exception occurs
     */
    public List<Note> getIssueNotes(Object projectIdOrPath, Integer issueIid) throws GitLabApiException {
        return (getIssueNotes(projectIdOrPath, issueIid, getDefaultPerPage()).all());
    }

    /**
     * Get a list of the issue's notes using the specified page and per page settings.
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/issues/:issue_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param issueIid        the issue IID to get the notes for
     * @param page            the page to get
     * @param perPage         the number of notes per page
     * @return the list of notes in the specified range
     * @throws GitLabApiException if any exception occurs
     */
    public List<Note> getIssueNotes(Object projectIdOrPath, Integer issueIid, int page, int perPage) throws GitLabApiException {
        Response response = get(Response.Status.OK, getPageQueryParams(page, perPage), "projects",
                getProjectIdOrPath(projectIdOrPath), "issues", issueIid, "notes");
        return (response.readEntity(new GenericType<List<Note>>() {
        }));
    }

    /**
     * Get a Pager of issues's notes.
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/issues/:issue_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param issueIid        the issue IID to get the notes for
     * @param itemsPerPage    the number of notes per page
     * @return the list of notes in the specified range
     * @throws GitLabApiException if any exception occurs
     */
    public Pager<Note> getIssueNotes(Object projectIdOrPath, Integer issueIid, int itemsPerPage) throws GitLabApiException {
        return (new Pager<Note>(this, Note.class, itemsPerPage, null,
                "projects", getProjectIdOrPath(projectIdOrPath), "issues", issueIid, "notes"));
    }

    /**
     * Get a Stream of the issues's notes.
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/issues/:issue_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param issueIid        the issue ID to get the notes for
     * @return a Stream of the issues's notes
     * @throws GitLabApiException if any exception occurs
     */
    public Stream<Note> getIssueNotesStream(Object projectIdOrPath, Integer issueIid) throws GitLabApiException {
        return (getIssueNotes(projectIdOrPath, issueIid, getDefaultPerPage()).stream());
    }

    /**
     * Get the specified issues's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param issueIid        the issue IID to get the notes for
     * @param noteId          the ID of the Note to get
     * @return a Note instance for the specified IDs
     * @throws GitLabApiException if any exception occurs
     */
    public Note getIssueNote(Object projectIdOrPath, Integer issueIid, Integer noteId) throws GitLabApiException {
        Response response = get(Response.Status.OK, getDefaultPerPageParam(),
                "projects", getProjectIdOrPath(projectIdOrPath), "issues", issueIid, "notes", noteId);
        return (response.readEntity(Note.class));
    }

    /**
     * Create a issues's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance @param projectIdOrPath the project ID to create the issues for
     * @param issueIid        the issue IID to create the notes for
     * @param body            the content of note
     * @return the created Note instance
     * @throws GitLabApiException if any exception occurs
     */
    public Note createIssueNote(Object projectIdOrPath, Integer issueIid, String body) throws GitLabApiException {
        return (createIssueNote(projectIdOrPath, issueIid, body, null));
    }

    /**
     * Create a issues's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param issueIid        the issue IID to create the notes for
     * @param body            the content of note
     * @param createdAt       the created time of note
     * @return the created Note instance
     * @throws GitLabApiException if any exception occurs
     */
    public Note createIssueNote(Object projectIdOrPath, Integer issueIid, String body, Date createdAt) throws GitLabApiException {

        GitLabApiForm formData = new GitLabApiForm()
                .withParam("body", body, true)
                .withParam("created_at", createdAt);
        Response response = post(Response.Status.CREATED, formData,
                "projects", getProjectIdOrPath(projectIdOrPath), "issues", issueIid, "notes");
        return (response.readEntity(Note.class));
    }

    /**
     * Update the specified issues's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param issueIid        the issue IID to update the notes for
     * @param noteId          the ID of the node to update
     * @param body            the update content for the Note
     * @return the modified Note instance
     * @throws GitLabApiException if any exception occurs
     */
    public Note updateIssueNote(Object projectIdOrPath, Integer issueIid, Integer noteId, String body) throws GitLabApiException {

        GitLabApiForm formData = new GitLabApiForm().withParam("body", body, true);
        Response response = put(Response.Status.OK, formData.asMap(),
                "projects", getProjectIdOrPath(projectIdOrPath), "issues", issueIid, "notes", noteId);
        return (response.readEntity(Note.class));
    }

    /**
     * Delete the specified issues's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param issueIid        the issue IID to delete the notes for
     * @param noteId          the ID of the node to delete
     * @throws GitLabApiException if any exception occurs
     */
    public void deleteIssueNote(Object projectIdOrPath, Integer issueIid, Integer noteId) throws GitLabApiException {

        if (issueIid == null) {
            throw new RuntimeException("issueIid cannot be null");
        }

        if (noteId == null) {
            throw new RuntimeException("noteId cannot be null");
        }

        Response.Status expectedStatus = (isApiVersion(GitLabApi.ApiVersion.V3) ? Response.Status.OK : Response.Status.NO_CONTENT);
        delete(expectedStatus, getDefaultPerPageParam(),
                "projects", getProjectIdOrPath(projectIdOrPath), "issues", issueIid, "notes", noteId);
    }

    /**
     * Gets a list of all notes for a single merge request
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/merge_requests/:merge_request_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the issue ID to get the notes for
     * @return a list of the merge request's notes
     * @throws GitLabApiException if any exception occurs
     */
    public List<Note> getMergeRequestNotes(Object projectIdOrPath, Integer mergeRequestIid) throws GitLabApiException {
        return (getMergeRequestNotes(projectIdOrPath, mergeRequestIid, null, null, getDefaultPerPage()).all());
    }

    /**
     * Gets a list of all notes for a single merge request.
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/merge_requests/:merge_request_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the issue ID to get the notes for
     * @param sortOrder       return merge request notes sorted in the specified sort order, default is DESC
     * @param orderBy         return merge request notes ordered by CREATED_AT or UPDATED_AT, default is CREATED_AT
     * @return a list of the merge request's notes
     * @throws GitLabApiException if any exception occurs
     */
    public List<Note> getMergeRequestNotes(Object projectIdOrPath, Integer mergeRequestIid, SortOrder sortOrder, Note.OrderBy orderBy) throws GitLabApiException {
        return (getMergeRequestNotes(projectIdOrPath, mergeRequestIid, sortOrder, orderBy, getDefaultPerPage()).all());
    }

    /**
     * Gets a list of all notes for a single merge request using the specified page and per page settings.
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/merge_requests/:merge_request_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the merge request IID to get the notes for
     * @param page            the page to get
     * @param perPage         the number of notes per page
     * @return the list of notes in the specified range
     * @throws GitLabApiException if any exception occurs
     */
    public List<Note> getMergeRequestNotes(Object projectIdOrPath, Integer mergeRequestIid, int page, int perPage) throws GitLabApiException {
        return (getMergeRequestNotes(projectIdOrPath, mergeRequestIid, null, null, page, perPage));
    }

    /**
     * Gets a list of all notes for a single merge request using the specified page and per page settings.
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/merge_requests/:merge_request_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the merge request IID to get the notes for
     * @param sortOrder       return merge request notes sorted in the specified sort order, default is DESC
     * @param orderBy         return merge request notes ordered by CREATED_AT or UPDATED_AT, default is CREATED_AT
     * @param page            the page to get
     * @param perPage         the number of notes per page
     * @return the list of notes in the specified range
     * @throws GitLabApiException if any exception occurs
     */
    public List<Note> getMergeRequestNotes(Object projectIdOrPath, Integer mergeRequestIid,
                                           SortOrder sortOrder, Note.OrderBy orderBy, int page, int perPage) throws GitLabApiException {

        GitLabApiForm formData = new GitLabApiForm()
                .withParam("sort", sortOrder)
                .withParam("order_by", orderBy)
                .withParam(PAGE_PARAM, page)
                .withParam(PER_PAGE_PARAM, perPage);
        Response response = get(Response.Status.OK, formData.asMap(),
                "projects", getProjectIdOrPath(projectIdOrPath), "merge_requests", mergeRequestIid, "notes");
        return (response.readEntity(new GenericType<List<Note>>() {
        }));
    }

    /**
     * Get a Pager of all notes for a single merge request
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/merge_requests/:merge_request_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the merge request IID to get the notes for
     * @param itemsPerPage    the number of notes per page
     * @return the list of notes in the specified range
     * @throws GitLabApiException if any exception occurs
     */
    public Pager<Note> getMergeRequestNotes(Object projectIdOrPath, Integer mergeRequestIid, int itemsPerPage) throws GitLabApiException {
        return (getMergeRequestNotes(projectIdOrPath, mergeRequestIid, null, null, itemsPerPage));
    }

    /**
     * Gets a Stream of all notes for a single merge request
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/merge_requests/:merge_request_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the issue ID to get the notes for
     * @return a Stream of the merge request's notes
     * @throws GitLabApiException if any exception occurs
     */
    public Stream<Note> getMergeRequestNotesStream(Object projectIdOrPath, Integer mergeRequestIid) throws GitLabApiException {
        return (getMergeRequestNotes(projectIdOrPath, mergeRequestIid, null, null, getDefaultPerPage()).stream());
    }

    /**
     * Get a Pager of all notes for a single merge request
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/merge_requests/:merge_request_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the merge request IID to get the notes for
     * @param sortOrder       return merge request notes sorted in the specified sort order, default is DESC
     * @param orderBy         return merge request notes ordered by CREATED_AT or UPDATED_AT, default is CREATED_AT
     * @param itemsPerPage    the number of notes per page
     * @return the list of notes in the specified range
     * @throws GitLabApiException if any exception occurs
     */
    public Pager<Note> getMergeRequestNotes(Object projectIdOrPath, Integer mergeRequestIid,
                                            SortOrder sortOrder, Note.OrderBy orderBy, int itemsPerPage) throws GitLabApiException {

        GitLabApiForm formData = new GitLabApiForm()
                .withParam("sort", sortOrder)
                .withParam("order_by", orderBy)
                .withParam(PAGE_PARAM, 1)
                .withParam(PER_PAGE_PARAM, itemsPerPage);
        return (new Pager<Note>(this, Note.class, itemsPerPage, formData.asMap(),
                "projects", getProjectIdOrPath(projectIdOrPath), "merge_requests", mergeRequestIid, "notes"));
    }

    /**
     * Gets a Stream of all notes for a single merge request.
     *
     * <pre><code>GitLab Endpoint: GET /projects/:id/merge_requests/:merge_request_iid/notes</code></pre>
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the issue ID to get the notes for
     * @param sortOrder       return merge request notes sorted in the specified sort order, default is DESC
     * @param orderBy         return merge request notes ordered by CREATED_AT or UPDATED_AT, default is CREATED_AT
     * @return a Stream of the merge request's notes
     * @throws GitLabApiException if any exception occurs
     */
    public Stream<Note> getMergeRequestNotesStream(Object projectIdOrPath, Integer mergeRequestIid, SortOrder sortOrder, Note.OrderBy orderBy) throws GitLabApiException {
        return (getMergeRequestNotes(projectIdOrPath, mergeRequestIid, sortOrder, orderBy, getDefaultPerPage()).stream());
    }

    /**
     * Get the specified merge request's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the merge request IID to get the notes for
     * @param noteId          the ID of the Note to get
     * @return a Note instance for the specified IDs
     * @throws GitLabApiException if any exception occurs
     */
    public Note getMergeRequestNote(Object projectIdOrPath, Integer mergeRequestIid, Integer noteId) throws GitLabApiException {
        Response response = get(Response.Status.OK, getDefaultPerPageParam(),
                "projects", getProjectIdOrPath(projectIdOrPath), "merge_requests", mergeRequestIid, "notes", noteId);
        return (response.readEntity(Note.class));
    }

    /**
     * Create a merge request's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the merge request IID to create the notes for
     * @param body            the content of note
     * @return the created Note instance
     * @throws GitLabApiException if any exception occurs
     */
    public Note createMergeRequestNote(Object projectIdOrPath, Integer mergeRequestIid, String body) throws GitLabApiException {
        GitLabApiForm formData = new GitLabApiForm().withParam("body", body, true);
        Response response = post(Response.Status.CREATED, formData,
                "projects", getProjectIdOrPath(projectIdOrPath), "merge_requests", mergeRequestIid, "notes");
        return (response.readEntity(Note.class));
    }

    /**
     * Update the specified merge request's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the merge request IID to update the notes for
     * @param noteId          the ID of the node to update
     * @param body            the update content for the Note
     * @return the modified Note instance
     * @throws GitLabApiException if any exception occurs
     */
    public Note updateMergeRequestNote(Object projectIdOrPath, Integer mergeRequestIid, Integer noteId, String body) throws GitLabApiException {

        GitLabApiForm formData = new GitLabApiForm().withParam("body", body, true);
        Response response = put(Response.Status.OK, formData.asMap(),
                "projects", getProjectIdOrPath(projectIdOrPath), "merge_requests", mergeRequestIid, "notes", noteId);
        return (response.readEntity(Note.class));
    }

    /**
     * Delete the specified merge request's note.
     *
     * @param projectIdOrPath the project in the form of an Integer(ID), String(path), or Project instance
     * @param mergeRequestIid the merge request IID to delete the notes for
     * @param noteId          the ID of the node to delete
     * @throws GitLabApiException if any exception occurs
     */
    public void deleteMergeRequestNote(Object projectIdOrPath, Integer mergeRequestIid, Integer noteId) throws GitLabApiException {

        if (mergeRequestIid == null) {
            throw new RuntimeException("mergeRequestIid cannot be null");
        }

        if (noteId == null) {
            throw new RuntimeException("noteId cannot be null");
        }

        Response.Status expectedStatus = (isApiVersion(GitLabApi.ApiVersion.V3) ? Response.Status.OK : Response.Status.NO_CONTENT);
        delete(expectedStatus, null,
                "projects", getProjectIdOrPath(projectIdOrPath), "merge_requests", mergeRequestIid, "notes", noteId);
    }
}
