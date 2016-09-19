function fnFormatDetails ( oTable, nTr )
{
    var aData = oTable.fnGetData( nTr );
    var sOut = '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:50px;">';
    sOut += '<tr><td>Name Node:</td><td>'+aData[1]+' '+aData[4]+'</td></tr>';
    sOut += '<tr><td>DataNode:</td><td>10台</td></tr>';
    sOut += '<tr><td>Master</td><td>2 台</td></tr>';
    sOut += '</table>';

    return sOut;
}


            function editRow(oTable, nRow,myid) {
                var aData = oTable.fnGetData(nRow);
                var jqTds = $('>td', nRow);
                jqTds[1].innerHTML = '<input type="text" class="form-control1 small" value="' + aData[1] + '">';
                jqTds[2].innerHTML = '<input type="text" class="form-control1 small" value="' + aData[2] + '">';
                jqTds[3].innerHTML = '<select style="width:100%;" multiple><option>Apple</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option><option>Apple2</option></select>';
                jqTds[4].innerHTML = '<input type="text" class="form-control1 small" value="' + aData[4] + '">';
                jqTds[5].innerHTML = '<a myid='+myid+' class="edit" href="">保存</a>';
                jqTds[6].innerHTML = '<a class="cancel" href="">取消</a>';
            }



                                    function restoreRow(oTable, nRow) {
                                        var aData = oTable.fnGetData(nRow);
                                        var jqTds = $('>td', nRow);

                                        for (var i = 0, iLen = jqTds.length; i < iLen; i++) {
                                            oTable.fnUpdate(aData[i], nRow, i, false);
                                        }

                                        oTable.fnDraw();
                                    }


function initdata() {

    $('#dynamic-table').dataTable( {
        "aaSorting": [[ 4, "desc" ]]
    } );

    /*
     * Insert a 'details' column to the table
     */
    var nCloneTh = document.createElement( 'th' );
    var nCloneTd = document.createElement( 'td' );
    nCloneTd.innerHTML = '<img src="/assets/images/plus.png">';
    nCloneTd.className = "center";

    $('#editable-sample thead tr').each( function () {
        this.insertBefore( nCloneTh, this.childNodes[0] );
    } );

    $('#editable-sample tbody tr').each( function () {
        this.insertBefore(  nCloneTd.cloneNode( true ), this.childNodes[0]);
    } );

    /*
     * Initialse DataTables, with no sorting on the 'details' column
     */
    var oTable = $('#editable-sample').dataTable( {
        "aoColumnDefs": [
            { "bSortable": false, "aTargets": [ 0 ] ,sDefaultContent: '',  aTargets: [ '_all' ] }
        ],
        "aaSorting": [[1, 'asc']]
    });

    /* Add event listener for opening and closing details
     * Note that the indicator for showing which row is open is not controlled by DataTables,
     * rather it is done here
     */
    $(document).on('click','#editable-sample tbody td img',function () {
        var nTr = $(this).parents('tr')[0];
        if ( oTable.fnIsOpen(nTr) )
        {
            /* This row is already open - close it */
            this.src = "/assets/images/plus.png";
            oTable.fnClose( nTr );
        }
        else
        {
            /* Open this row */
            this.src = "/assets/images/minus.png";
            //oTable.fnOpen( nTr, fnFormatDetails(oTable, nTr), 'details' );

                        /*详情*/
                        $(document).ready(function(){
                            $.ajax({
                                type:"GET",
                                url:"/nodedata",
                                data:{pid:11},
                                dataType:"json",
                                success:function(data){
                                sOut= "<table cellpadding=\"5\" cellspacing=\"0\" border=\"0\" style=\"padding-left:50px;\"><thead><tr><th>IP列表</th><th>host列表</th><th>角色</th></thead><tbody>";
                                    var myobj=eval(data);
                                    for (var p in myobj) {
                                        var datamid = eval(data[p]);
                                        for (var i = 0; i < datamid.length; i++) {
                                             sOut +="<tr><td><font color=\"#ffffff\">" + datamid[i].ip + "</font></td><td><font color=\"#ffffff\">" + datamid[i].host + "</font></td><td><font color=\"#ffffff\">" + datamid[i].role + "</font></td></tr>";

                                        };
                                         sOut += '<tr><td><font color=\"#ffffff\">Rendering engine:</font></td><td>'+'30台机器'+'</td></tr>';
                                         sOut += '<tr><td><font color=\"#ffffff\">Link to source:</font></td><td>Could provide a link here</td></tr>';
                                         sOut += '<tr><td><font color=\"#ffffff\">Extra info:</font></td><td>And any further details here (images etc)</td></tr>';
                                    };
                                    sOut += "</tbody></table>";
                                    oTable.fnOpen( nTr, sOut, 'details' );

                                }
                            });
                        });

        }
    } );

                var nEditing = null;

                //新增
                $('#editable-sample_new').click(function (e) {
                    e.preventDefault();
                    var aiNew = oTable.fnAddData(['', '', '', '',
                            '<a class="edit" href="">编辑</a>', '<a class="cancel" data-mode="new" href="">取消</a>'
                    ]);
                    var nRow = oTable.fnGetNodes(aiNew[0]);
                    editRow(oTable, nRow,0);
                    nEditing = nRow;
                });

                          //保存
                            $('#editable-sample a.edit').live('click', function (e) {
                                e.preventDefault();

                                /* Get the row as a parent of the link that was clicked on */
                                var nRow = $(this).parents('tr')[0];

                                if (nEditing !== null && nEditing != nRow) {
                                    /* Currently editing - but not this row - restore the old before continuing to edit mode */
                                    restoreRow(oTable, nEditing);
                                    editRow(oTable, nRow,0);
                                    nEditing = nRow;
                                } else if (nEditing == nRow && this.innerHTML == "保存") {
                                    /* Editing this row and want to save it */
                                           var v_id = $(e.target).attr('myid');

                                            var jqInputs = $('input', nEditing);

                                            var jqSelect = $('select', nEditing);

                                                    var $name = jqInputs[0].value;
                                                    var $unit = jqInputs[1].value;

                                                        jqSelect.val()
                                                    var _list = arrayToJson(jqSelect.val())


                                                    var $url = jqInputs[2].value;
                                                    $(document).ready(function(){
                                                            $.ajax({
                                                                type:"GET",
                                                                url:"/update",
                                                                data:{id:v_id,name:$name,unit:$unit,version:_list,url:$url},
                                                                dataType:"text",
                                                                success:function(data){
                                                                            swal({
                                                                                                                              title: "操作完成!",
                                                                                                                              text: data+" /集群信息更新成功",
                                                                                                                              type: "success",
                                                                                                                              showCancelButton: false,
                                                                                                                              confirmButtonText: "确定",
                                                                                                                              closeOnConfirm: true,
                                                                                                                              closeOnCancel: false },
                                                                                                                              function(isConfirm){
                                                                                                                                        window.location="/metadata";
                                                                                                                                        nEditing = null;
                                                                                                                                    });


                                                                }
                                                            });
                                                    });

                                } else {
                                    /* No edit in progress - let's start one */
                                    var v_id = $(e.target).attr('myid');
                                    editRow(oTable, nRow,v_id);
                                    nEditing = nRow;
                                }
                            });




                                        //删除
                                        $('#editable-sample a.delete').live('click', function (e) {
                                            e.preventDefault();
                                                        var nRow = $(this).parents('tr')[0];
                                                          var $thisTD = $(this).parents('tr').find("td:eq(0)");

                                                            var $v_id = $(e.target).attr('myid');
                                                                 swal({
                                                                    title: "请确认?",
                                                                    text: "确定删除 "+$(this).parents('tr').find("td:eq(0)").text()+"?",
                                                                    type: "warning",
                                                                    confirmButtonColor: "#DD6B55",
                                                                    confirmButtonText: "确认",
                                                                    cancelButtonText: "取消",
                                                                    showCancelButton: true,
                                                                    closeOnConfirm: false,
                                                                     },
                                                                    function(isConfirm){
                                                                    if (isConfirm) {

                                                                        $(document).ready(function(){
                                                                            $.ajax({
                                                                                type:"GET",
                                                                                url:"/rm",
                                                                                data:{id:$v_id},
                                                                                dataType:"text",
                                                                                success:function(data){
                                                                                     oTable.fnDeleteRow(nRow);
                                                                                     swal("操作成功", $thisTD.text()+" "+data, "success");

                                                                                }
                                                                            });
                                                                        });

                                                                       }  });

                                        });



}


/**
array 转json
*/
function arrayToJson(o) {
    var r = [];
    if (typeof o == "string") return "\"" + o.replace(/([\'\"\\])/g, "\\$1").replace(/(\n)/g, "\\n").replace(/(\r)/g, "\\r").replace(/(\t)/g, "\\t") + "\"";
    if (typeof o == "object") {
      if (!o.sort) {
        for (var i in o)
          r.push(i + ":" + arrayToJson(o[i]));
        if (!!document.all && !/^\n?function\s*toString\(\)\s*\{\n?\s*\[native code\]\n?\s*\}\n?\s*$/.test(o.toString)) {
          r.push("toString:" + o.toString.toString());
        }
        r = "{" + r.join() + "}";
      } else {
        for (var i = 0; i < o.length; i++) {
          r.push(arrayToJson(o[i]));
        }
        r = "[" + r.join() + "]";
      }
      return r;
    }
    return o.toString();
  }