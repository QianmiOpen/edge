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
    selectClass : function(cls) {
	if (cls) {
	    // xxx.xxx.xxx
	    var parent = cls.slice(0, cls.lastIndexOf('.'));
	    this.selectPath('/root/apidocs/' + parent + '/' + cls);
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
	var ps = this.cclass.split('.');
	this.title = ps[ps.length - 1];
	this.tabTip = ps;
	var formPanel = new Ext.form.FormPanel(
		{
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
			value : this.cclass,
			readOnly : true,
			name : 'methodName'
		    }, {
			fieldLabel : 'Params',
			name : 'params',
			xtype : 'textarea',
			value : this.defParam,
			height : 350,
			allowBlank : true
		    } ],
		    buttons : [ {
			text : 'Submit',
			type : 'submit',
			scope : this,
			handler : function() {
			    var frm = this.formPanel.getForm();
			    var paramField = frm.findField("params");
			    var formatParam = js_beautify(
				    paramField.getValue(), 4, ' ');
			    paramField.setValue(formatParam);

			    var resultArea = this.resultArea;
			    frm.submit({
				waitMsg : '正在提交数据',
				waitTitle : '提示',
				url : 'executeTest.do',
				method : 'GET',
				success : function(form, action) {

				    var result = js_beautify(action.result.msg,
					    4, ' ');
				    resultArea.setValue(result);
				},
				failure : function(form, action) {
				    Ext.Msg.alert('提示', '原因如下：'
					    + action.result.errors.info);
				}
			    });
			}
		    } ]
		});

	this.resultArea = new Ext.form.TextArea({
	    fieldLabel : 'Result',
	    xtype : 'textarea',
	    height : 350,
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

    loadClass : function(href, cls) {
	var id = 'docs-' + cls;
	var tab = this.getComponent(id);
	if (tab) {
	    this.setActiveTab(tab);
	} else {
	    var me = this;
	    Ext.Ajax.request({
		url : 'getParamDesc.do',
		method : 'post',
		params : {
		    methodName : cls
		},
		success : function(response, options) {
		    var o = Ext.util.JSON.decode(response.responseText);
		    var defParam = js_beautify(o.msg, 4, ' ');
		    var p = me.add(new DocPanel({
			id : id,
			cclass : cls,
			defParam : defParam,
			// autoLoad: autoLoad,
			iconCls : Docs.icons[cls]
		    }));
		    me.setActiveTab(p);
		},
		failure : function() {
		    var p = me.add(new DocPanel({
			id : id,
			cclass : cls,
			// autoLoad: autoLoad,
			iconCls : Docs.icons[cls]
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
	    mainPanel.loadClass(node.attributes.href, node.id);
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
	    if (!n.attributes.isClass && n.ui.ctNode.offsetHeight < 3) {
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