/*!
 * Ext JS Library 3.0.0
 * Copyright(c) 2006-2009 Ext JS, LLC
 * licensing@extjs.com
 * http://www.extjs.com/license
 */
Ext.BLANK_IMAGE_URL = "./resources/images/default/s.gif";

Docs = {};

ApiPanel = function() {
    ApiPanel.superclass.constructor.call(this, {
        id : 'api-tree',
        region : 'west',
        split : true,
        width : 280,
        minSize : 175,
        maxSize : 500,
        collapsible : true,
        margins : '0 0 5 5',
        cmargins : '0 0 0 0',
        rootVisible : false,
        lines : false,
        autoScroll : true,
        animCollapse : false,
        animate : true,
        enableDD : true,
        containerScroll : true,
        collapseMode : 'mini',
        root : new Ext.tree.AsyncTreeNode({
            text : 'API Test',
            id : 'root',
            singleClickExpand : true,
            expanded : true
        }),
        loader : new Ext.tree.TreeLoader({
            preloadChildren : true,
            clearOnLoad : false,
            dataUrl : 'getApiList.do'
        }),
        collapseFirst : false
    });
    
    this.getSelectionModel().on('beforeselect', function(sm, node) {
        return node.isLeaf();
    });
    
};

Ext.extend(ApiPanel, Ext.tree.TreePanel, {
    selectClass : function(fullPath) {
        if (fullPath) {
            // xxx.xxx.xxx
            this.selectPath('/root/' + fullPath);
        }
    }
});

DocPanel = Ext.extend(Ext.Panel, {
    closable : true,
    autoScroll : true,
    layout : 'column',
    margins : '35 5 5 0',
    containerScroll : true,
    initComponent : function() {
//        var serviceURLCombo = new Ext.form.ComboBox();

        var formPanel = new Ext.form.FormPanel({
            labelWidth : 75,
            border : false,
            defaults : {
                // 应用于每个被包含的项 applied to each contained item
                width : 370,
                msgTarget : 'side'
            },
            layoutConfig : {
                // 这里是布局配置项 layout-specific configs go here
                labelSeparator : ':'
            },
            items : [ {
                xtype : 'textfield',
                fieldLabel : 'Method',
                value : this.serviceKey + "@" + this.title,
                readOnly : true,
                name : 'methodName'
            }, {
                xtype : 'combo',
                fieldLabel : 'ServiceURL', // UI标签名称
                name : 'serviceUrl', // 作为form提交时传送的参数名
                allowBlank : true, // 是否允许为空
                mode : 'local', // 数据模式, local为本地模式, 如果不设置,就显示不停的加载中...
                readOnly : true, // 是否只读
                triggerAction : 'all', // 显示所有下列数.必须指定为'all'
                // width:330,
                anchor : '90%',
                emptyText : '', // 没有默认值时,显示的字符串
                store : new Ext.data.SimpleStore({ // 填充的数据
                    fields : [ 'text', 'value' ],
                    data : this.serviceUrls
                }),
                value : '', // 设置当前选中的值, 也可用作初始化时的默认值, 默认为空
                valueField : 'value', // 传送的值
                displayField : 'text' // UI列表显示的文本
            }, {
                fieldLabel : 'Params',
                name : 'params',
                xtype : 'textarea',
                value : this.defParam,
                height : 370,
                listeners:{
                    scope: this,
                    'blur': function(){
                        var frm = this.formPanel.getForm();
                        var paramField = frm.findField("params");
                        var formatParam;
                        try{
                            // Json 格式化
                            formatParam = js_beautify(paramField.getValue(), 4, ' ');
                        } catch(err) {
                            return;
                        }
                        paramField.setValue(formatParam);
                    }
                },
                allowBlank : true
            } ],
            buttons : [ {
                text : 'Submit',
                type : 'submit',
                scope : this,
                handler : function() {
                    var frm = this.formPanel.getForm();
                    var paramField = frm.findField("params");
                    var resultArea = this.resultArea;

                    var formatParam;
                    try{
                        // Json 格式化
                        formatParam = js_beautify(paramField.getValue(), 4, ' ');
                    } catch(err) {
                        resultArea.setValue("JSON格式错误");
                        return;
                    }
                    paramField.setValue(formatParam);

                    frm.submit({
                        waitMsg : '正在提交数据',
                        waitTitle : '提示',
                        url : 'executeTest.do',
                        method : 'GET',
                        success : function(form, action) {
                            var result;
                            try{
                                // Json 格式化
                                result = js_beautify(action.result.msg, 4, ' ');
                            } catch(err) {
                                result = action.result.msg;
                            }
                            resultArea.setValue(result);
                        },
                        failure : function(form, action) {
                            Ext.Msg.alert('提示', '原因如下：' + action.result.errors.info);
                        }
                    });
                }
            } ]
        });
        
        this.resultArea = new Ext.form.TextArea({
            fieldLabel : 'Result',
            xtype : 'textarea',
            height : 390,
            name : 'result'
        });
        
        this.items = [ {
            columnWidth : .5,
            // baseCls:'x-plain',
            bodyStyle : 'padding:5px 0 5px 5px',
            border : false,
            items : [ formPanel ]
        }, {
            columnWidth : .5,
            // baseCls:'x-plain',
            bodyStyle : 'padding:5px 0 5px 5px',
            border : false,
            items : [ {
                // id: 'resultPanel',
                xtype : 'form',
                border : false,
                labelWidth : 75,
                bodyStyle : 'padding:25px',
                defaults : {
                    // 应用于每个被包含的项 applied to each contained item
                    width : 370,
                    msgTarget : 'side'
                },
                layoutConfig : {
                    // 这里是布局配置项 layout-specific configs go here
                    labelSeparator : ':'
                },
                items : [ this.resultArea ]
            } ]
        } ];
        
        this.formPanel = formPanel;
        
        DocPanel.superclass.initComponent.call(this);
    }
});

MainPanel = function() {
    
    MainPanel.superclass.constructor.call(this, {
        id : 'doc-body',
        region : 'center',
        margins : '0 5 5 0',
        resizeTabs : true,
        minTabWidth : 135,
        tabWidth : 135,
        plugins : new Ext.ux.TabCloseMenu(),
        enableTabScroll : true,
        activeTab : 0,
        
        items : {
            id : 'welcome-panel',
            title : 'API Home',
            autoLoad : {
                url : 'welcome.html',
                scope : this
            },
            iconCls : 'icon-docs',
            autoScroll : true
        }
    });
};

Ext.extend(MainPanel, Ext.TabPanel, {
    
    initEvents : function() {
        MainPanel.superclass.initEvents.call(this);
    },

    // node.attributes.href, node.text, node.attributes.fullPath, node.attributes.serviceKey, node.attributes.fullText
    loadClass : function(node) {
        var id = 'docs-' + node.id;
        var tab = this.getComponent(id);
        if (tab) {
            this.setActiveTab(tab);
        } else {
            var nodeAttr = node.attributes;
            var fullPath = nodeAttr.fullPath;
            var serviceKey = nodeAttr.serviceKey;
            var fullText = nodeAttr.fullText;
            var tabTitle = node.text;

            var me = this;
            Ext.Ajax.request({
                url : 'getParamDesc.do',
                method : 'post',
                params : {
                    serviceKey : serviceKey,
                    methodName : node.text
                },
                success : function(response, options) {
                    var o = Ext.util.JSON.decode(response.responseText);
                    var defParam = "";;
                    try{
                        // Json 格式化
                        defParam = o.paramDesc ? js_beautify(o.paramDesc, 4, ' ') : "";
                    } catch(err) {
                        defParam = o.paramDesc;
                    }

                    var serviceUrls = o.serviceUrls;
                    
                    var p = me.add(new DocPanel({
                        id : id,
                        cclass : fullPath,
                        defParam : defParam,
                        serviceKey : serviceKey,
                        serviceUrls : serviceUrls,
                        title : tabTitle,
                        tabTip : fullText,
                        // autoLoad: autoLoad,
                        iconCls : Docs.icons[tabTitle]
                    }));
                    me.setActiveTab(p);
                },
                failure : function() {
                    var p = me.add(new DocPanel({
                        id : id,
                        cclass : fullPath,
                        serviceKey : serviceKey,
                        serviceUrls : [[]],
                        title : tabTitle,
                        tabTip : fullText,
                        // autoLoad: autoLoad,
                        iconCls : Docs.icons[tabTitle]
                    }));
                    me.setActiveTab(p);
                }
            });
        }
    }

});

Ext.onReady(function() {
    
    Ext.QuickTips.init();
    
    var api = new ApiPanel();
    var mainPanel = new MainPanel();
    
    api.on('click', function(node, e) {
        if (node.isLeaf()) {
            e.stopEvent();
            if(node.id == 'ApiList'){ // id等于ApiList时不打开tab
                return;
            }
            mainPanel.loadClass(node);
        }
    });
    
    mainPanel.on('tabchange', function(tp, tab) {
        api.selectClass(tab.cclass);
    });
    
    var hd = new Ext.Panel({
        border : false,
        layout : 'anchor',
        region : 'north',
        cls : 'docs-header',
        height : 60,
        items : [ {
            xtype : 'box',
            el : 'header',
            border : false,
            anchor : 'none -25'
        }, new Ext.Toolbar({
            cls : 'top-toolbar',
            items : [ ' ', new Ext.form.TextField({
                width : 200,
                emptyText : 'Find a Class',
                listeners : {
                    render : function(f) {
                        f.el.on('keydown', filterTree, f, {
                            buffer : 350
                        });
                    }
                }
            }), ' ', ' ', {
                iconCls : 'icon-expand-all',
                tooltip : 'Expand All',
                handler : function() {
                    api.root.expand(true);
                }
            }, '-', {
                iconCls : 'icon-collapse-all',
                tooltip : 'Collapse All',
                handler : function() {
                    api.root.collapse(true);
                }
            } ]
        }) ]
    });
    
    var viewport = new Ext.Viewport({
        layout : 'border',
        items : [ hd, api, mainPanel ]
    });
    
    api.expandPath('/root/apidocs');
    
    viewport.doLayout();
    
    setTimeout(function() {
        Ext.get('loading').remove();
        Ext.get('loading-mask').fadeOut({
            remove : true
        });
    }, 250);
    
    var filter = new Ext.tree.TreeFilter(api, {
        clearBlank : true,
        autoClear : true
    });
    var hiddenPkgs = [];
    function filterTree(e) {
        var text = e.target.value;
        Ext.each(hiddenPkgs, function(n) {
            n.ui.show();
        });
        if (!text) {
            filter.clear();
            return;
        }
        api.expandAll();
        
        var re = new RegExp('^' + Ext.escapeRe(text), 'i');
        filter.filterBy(function(n) {
            return !n.attributes.isClass || re.test(n.text);
        });
        
        // hide empty packages that weren't filtered
        hiddenPkgs = [];
        api.root.cascade(function(n) {
            if (!n.attributes.isClass && n.ui.ctNode && n.ui.ctNode.offsetHeight < 3) {
                n.ui.hide();
                hiddenPkgs.push(n);
            }
        });
    }
    
});

Ext.Ajax.on('requestcomplete', function(ajax, xhr, o) {
    if (typeof urchinTracker == 'function' && o && o.url) {
        urchinTracker(o.url);
    }
});