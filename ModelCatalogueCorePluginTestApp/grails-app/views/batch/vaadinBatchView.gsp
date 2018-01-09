<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Batch of Actions ${id}</title>
</head>
<body>
    <iframe style="position:fixed; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%; border:none; margin:0; padding:0; overflow:hidden; z-index:999999;" src="${grailsApplication.config.grails.serverURL}/vaadinApp/#!batch/batchId=${id}"></iframe>
</body>
</html>
