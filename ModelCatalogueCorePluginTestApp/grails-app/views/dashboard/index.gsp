<%@ page import="org.modelcatalogue.core.view.DataElementViewModel; org.modelcatalogue.core.view.DataModelViewModel; org.modelcatalogue.core.util.MetadataDomain; org.modelcatalogue.core.dashboard.DashboardDropdown; org.modelcatalogue.core.util.PublishedStatus;" %>
<html>
<head>
    <title><g:message code="dashboard.title" default="Data Models"/></title>
    <meta name="layout" content="main" />
</head>
<body>
<div class="panel panel-default">
    <div class="panel-heading">
        <g:form controller="dashboard" action="index" method="GET">
            <div>
                <div class="input-group">
                    <g:textField name="search" value="${search}" aria-label="..."/>
                    <g:select name="metadataDomain" from="${metadataDomainList}" value="${metadataDomain}"/>
                    <g:select name="status" from="${DashboardDropdown.values()}" value="${status}"/>
                    <input type="submit" class="btn btn-default" value="${g.message(code:'datamodel.filter', default: 'Filter')}" />
                </div><!-- /input-group -->

            </div><!-- /input-group -->
        </g:form>
    </div><!-- /.panel-heading -->
</div><!-- /.panel-default -->

<g:render template="/templates/flashmessage" />
<g:render template="/templates/flasherror" />

<div class="panel-body">
<g:if test="${catalogueElementList}">
    <g:if test="${catalogueElementList.first() instanceof DataModelViewModel}">
        <g:render template="dataModelViewTable"/>
    </g:if>
    <g:elseif test="${catalogueElementList.first() instanceof DataElementViewModel}">
        <g:render template="dataElementViewTable"/>
    </g:elseif>
    <div class="pagination">
        <g:paginate total="${total ?: 0}" params="${[max: paginationQuery?.offset,
                                                     metadataDomain: metadataDomain,
                                                     status: status,
                                                     search: search,
                                                     order: sortQuery?.order,
                                                     sort: sortQuery?.sort,
                                                     offset: paginationQuery?.offset]}"/>
    </div>
    </g:if>
    <g:else>
        <h1><g:message code="datamodel.notFound" default="Data Models not found"/></h1>
    </g:else>
</div><!-- /.panel-body -->
</body>
</html>
