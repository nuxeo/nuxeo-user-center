/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.center.dashboard.operations;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.jbpm.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.jbpm.providers.DocumentProcessPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Operation to retrieve the tasks waiting for the current user.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
@Operation(id = DocumentTaskPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "DocumentTaskPageProvider", description = "Returns the tasks waiting for the current user.")
public class DocumentTaskPageProviderOperation extends
        AbstractWorkflowOperation {

    public static final String ID = "Document.TaskPageProvider";

    public static final String USER_TASKS_PAGE_PROVIDER = "USER_TASKS";

    @Param(name = "language", required = false)
    protected String language;

    @Param(name = "page", required = false)
    protected Integer page = 0;

    @Context
    protected CoreSession session;

    @Context
    protected DocumentViewCodecManager documentViewCodecManager;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public Blob run() throws Exception {
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(DocumentProcessPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        PageProviderService pps = Framework.getLocalService(PageProviderService.class);
        PageProvider<DashBoardItem> pageProvider = (PageProvider<DashBoardItem>) pps.getPageProvider(
                USER_TASKS_PAGE_PROVIDER, null, null, (long) page, props);

        Locale locale = language != null && !language.isEmpty() ? new Locale(
                language) : Locale.ENGLISH;

        JSONArray processes = new JSONArray();
        for (DashBoardItem dashBoardItem : pageProvider.getCurrentPage()) {
            boolean createdFromCreateTaskOperation = Boolean.parseBoolean((String) dashBoardItem.getTaskInstance().getVariableLocally(
                    "createdFromCreateTaskOperation"));

            JSONObject obj = new JSONObject();
            obj.put("taskName",
                    createdFromCreateTaskOperation ? dashBoardItem.getName()
                            : getI18nTaskName(dashBoardItem.getName(), locale));
            obj.put("directive",
                    getI18nLabel(dashBoardItem.getDirective(), locale));
            obj.put("comment", dashBoardItem.getComment());
            Date dueDate = dashBoardItem.getDueDate();
            obj.put("dueDate",
                    dueDate != null ? DateParser.formatW3CDateTime(dueDate)
                            : "");
            obj.put("documentTitle", dashBoardItem.getDocument().getTitle());
            obj.put("documentLink",
                    getDocumentLink(documentViewCodecManager,
                            dashBoardItem.getDocument(),
                            !createdFromCreateTaskOperation));
            Date startDate = dashBoardItem.getStartDate();
            obj.put("startDate",
                    startDate != null ? DateParser.formatW3CDateTime(startDate)
                            : "");
            processes.add(obj);
        }

        JSONObject json = new JSONObject();
        json.put("isPaginable", true);
        json.put("totalSize", pageProvider.getResultsCount());
        json.put("pageIndex", pageProvider.getCurrentPageIndex());
        json.put("pageSize", pageProvider.getPageSize());
        json.put("pageCount", pageProvider.getNumberOfPages());

        json.put("entries", processes);
        return new InputStreamBlob(new ByteArrayInputStream(
                json.toString().getBytes("UTF-8")), "application/json");
    }

    protected String getI18nTaskName(String taskName, Locale locale) {
        String labelKey = "label.workflow.task." + taskName;
        return getI18nLabel(labelKey, locale);
    }

}