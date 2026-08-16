-- AI Platform 1.0.0 公开基线：public schema 的完整结构、约束与索引。

CREATE TABLE public.sys_calendar (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    calendar text,
    year integer NOT NULL
);


--
-- Name: TABLE sys_calendar; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_calendar IS '日历表';


--
-- Name: COLUMN sys_calendar.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.id IS '主键';


--
-- Name: COLUMN sys_calendar.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.create_by IS '创建人ID';


--
-- Name: COLUMN sys_calendar.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.create_time IS '创建时间';


--
-- Name: COLUMN sys_calendar.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.deleted IS '软删除标志';


--
-- Name: COLUMN sys_calendar.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.update_by IS '更新人ID';


--
-- Name: COLUMN sys_calendar.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.update_time IS '更新时间';


--
-- Name: COLUMN sys_calendar.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_calendar.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.enabled IS '是否启用';


--
-- Name: COLUMN sys_calendar.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.remark IS '备注';


--
-- Name: COLUMN sys_calendar.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.status IS '状态';


--
-- Name: COLUMN sys_calendar.calendar; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.calendar IS '日历JSON';


--
-- Name: COLUMN sys_calendar.year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_calendar.year IS '年份';


--
-- Name: sys_dept; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_dept (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    ancestors character varying(500),
    dept_code character varying(50),
    dept_name character varying(50) NOT NULL,
    email character varying(100),
    has_child boolean,
    leader_id character varying(64),
    leader_name character varying(50),
    location character varying(200),
    parent_id character varying(64),
    phone character varying(20),
    region_code character varying(20)
);


--
-- Name: TABLE sys_dept; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_dept IS '部门表';


--
-- Name: COLUMN sys_dept.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.id IS '主键';


--
-- Name: COLUMN sys_dept.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.create_by IS '创建人ID';


--
-- Name: COLUMN sys_dept.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.create_time IS '创建时间';


--
-- Name: COLUMN sys_dept.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.deleted IS '软删除标志';


--
-- Name: COLUMN sys_dept.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.update_by IS '更新人ID';


--
-- Name: COLUMN sys_dept.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.update_time IS '更新时间';


--
-- Name: COLUMN sys_dept.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_dept.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_dept.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.enabled IS '是否启用';


--
-- Name: COLUMN sys_dept.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.remark IS '备注';


--
-- Name: COLUMN sys_dept.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.status IS '状态';


--
-- Name: COLUMN sys_dept.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.sort_order IS '排序号';


--
-- Name: COLUMN sys_dept.ancestors; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.ancestors IS '祖级列表';




--
-- Name: COLUMN sys_dept.dept_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.dept_code IS '部门编码';


--
-- Name: COLUMN sys_dept.dept_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.dept_name IS '部门名称';


--
-- Name: COLUMN sys_dept.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.email IS '邮箱';


--
-- Name: COLUMN sys_dept.has_child; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.has_child IS '是否有子部门';




--
-- Name: COLUMN sys_dept.leader_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.leader_id IS '负责人ID';


--
-- Name: COLUMN sys_dept.leader_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.leader_name IS '负责人姓名';


--
-- Name: COLUMN sys_dept.location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.location IS '位置';


--
-- Name: COLUMN sys_dept.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.parent_id IS '父部门ID';


--
-- Name: COLUMN sys_dept.phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.phone IS '联系电话';


--
-- Name: COLUMN sys_dept.region_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dept.region_code IS '区域编码';


--
-- Name: sys_dict; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_dict (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    dict_label character varying(128) NOT NULL,
    dict_type character varying(64) NOT NULL,
    dict_value character varying(128) NOT NULL
);


--
-- Name: TABLE sys_dict; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_dict IS '字典表';


--
-- Name: COLUMN sys_dict.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.id IS '主键';


--
-- Name: COLUMN sys_dict.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.create_by IS '创建人ID';


--
-- Name: COLUMN sys_dict.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.create_time IS '创建时间';


--
-- Name: COLUMN sys_dict.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.deleted IS '软删除标志';


--
-- Name: COLUMN sys_dict.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.update_by IS '更新人ID';


--
-- Name: COLUMN sys_dict.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.update_time IS '更新时间';


--
-- Name: COLUMN sys_dict.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_dict.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_dict.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.enabled IS '是否启用';


--
-- Name: COLUMN sys_dict.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.remark IS '备注';


--
-- Name: COLUMN sys_dict.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.status IS '状态';


--
-- Name: COLUMN sys_dict.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.sort_order IS '排序号';


--
-- Name: COLUMN sys_dict.dict_label; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.dict_label IS '字典标签';


--
-- Name: COLUMN sys_dict.dict_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.dict_type IS '字典类型';


--
-- Name: COLUMN sys_dict.dict_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict.dict_value IS '字典值';


--
-- Name: sys_dict_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_dict_item (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    dict_code character varying(100),
    dict_id character varying(64),
    item_color character varying(20),
    item_css_class character varying(128),
    item_label character varying(100),
    item_selected boolean NOT NULL,
    item_style character varying(50),
    item_value character varying(100)
);


--
-- Name: TABLE sys_dict_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_dict_item IS '字典项表';


--
-- Name: COLUMN sys_dict_item.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.id IS '主键';


--
-- Name: COLUMN sys_dict_item.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.create_by IS '创建人ID';


--
-- Name: COLUMN sys_dict_item.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.create_time IS '创建时间';


--
-- Name: COLUMN sys_dict_item.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.deleted IS '软删除标志';


--
-- Name: COLUMN sys_dict_item.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.update_by IS '更新人ID';


--
-- Name: COLUMN sys_dict_item.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.update_time IS '更新时间';


--
-- Name: COLUMN sys_dict_item.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_dict_item.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_dict_item.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.enabled IS '是否启用';


--
-- Name: COLUMN sys_dict_item.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.remark IS '备注';


--
-- Name: COLUMN sys_dict_item.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.status IS '状态';


--
-- Name: COLUMN sys_dict_item.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.sort_order IS '排序号';


--
-- Name: COLUMN sys_dict_item.dict_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.dict_code IS '字典代码';


--
-- Name: COLUMN sys_dict_item.dict_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.dict_id IS '字典ID';


--
-- Name: COLUMN sys_dict_item.item_color; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.item_color IS '字典颜色';


--
-- Name: COLUMN sys_dict_item.item_css_class; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.item_css_class IS 'CSS样式类名';


--
-- Name: COLUMN sys_dict_item.item_label; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.item_label IS '字典项标签';


--
-- Name: COLUMN sys_dict_item.item_selected; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.item_selected IS '是否默认选中';


--
-- Name: COLUMN sys_dict_item.item_style; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.item_style IS '样式类型';


--
-- Name: COLUMN sys_dict_item.item_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_dict_item.item_value IS '字典项值';


--
-- Name: sys_import_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_import_template (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    file_type character varying(32) NOT NULL,
    template_code character varying(64) NOT NULL,
    template_module character varying(64) NOT NULL,
    template_name character varying(128) NOT NULL,
    batch_size integer DEFAULT 500 NOT NULL,
    custom_import_enabled boolean DEFAULT true NOT NULL,
    file_name_patterns text[],
    handler_key character varying(64),
    header_row_index integer DEFAULT 0 NOT NULL,
    sheet_names text[],
    target_schema character varying(64),
    target_table character varying(128),
    write_mode character varying(16) DEFAULT 'INSERT'::character varying NOT NULL
);


--
-- Name: TABLE sys_import_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_import_template IS '导入模板表';


--
-- Name: COLUMN sys_import_template.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.id IS '主键';


--
-- Name: COLUMN sys_import_template.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.create_by IS '创建人ID';


--
-- Name: COLUMN sys_import_template.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.create_time IS '创建时间';


--
-- Name: COLUMN sys_import_template.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.deleted IS '软删除标志';


--
-- Name: COLUMN sys_import_template.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.update_by IS '更新人ID';


--
-- Name: COLUMN sys_import_template.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.update_time IS '更新时间';


--
-- Name: COLUMN sys_import_template.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_import_template.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_import_template.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.enabled IS '是否启用';


--
-- Name: COLUMN sys_import_template.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.remark IS '备注';


--
-- Name: COLUMN sys_import_template.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.status IS '状态';


--
-- Name: COLUMN sys_import_template.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.sort_order IS '排序号';


--
-- Name: COLUMN sys_import_template.file_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.file_type IS '文件类型';




--
-- Name: COLUMN sys_import_template.template_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.template_code IS '模板编码';


--
-- Name: COLUMN sys_import_template.template_module; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.template_module IS '所属模块';


--
-- Name: COLUMN sys_import_template.template_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.template_name IS '模板名称';


--
-- Name: COLUMN sys_import_template.batch_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.batch_size IS '批处理行数';


--
-- Name: COLUMN sys_import_template.custom_import_enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.custom_import_enabled IS '是否使用自定义导入处理器';


--
-- Name: COLUMN sys_import_template.file_name_patterns; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.file_name_patterns IS '可匹配的文件名模式';


--
-- Name: COLUMN sys_import_template.handler_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.handler_key IS '自定义导入处理器键';


--
-- Name: COLUMN sys_import_template.header_row_index; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.header_row_index IS '表头行索引';


--
-- Name: COLUMN sys_import_template.sheet_names; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.sheet_names IS '可导入的Sheet名称';


--
-- Name: COLUMN sys_import_template.target_schema; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.target_schema IS '目标Schema';


--
-- Name: COLUMN sys_import_template.target_table; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.target_table IS '目标表名';


--
-- Name: COLUMN sys_import_template.write_mode; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template.write_mode IS '写入方式';


--
-- Name: sys_import_template_field; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_import_template_field (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    exact_match boolean NOT NULL,
    field character varying(128) NOT NULL,
    field_title text[] NOT NULL,
    multiple boolean NOT NULL,
    repeat boolean NOT NULL,
    required boolean NOT NULL,
    template_id character varying(64) NOT NULL,
    converter_key character varying(64),
    data_type character varying(32) DEFAULT 'STRING'::character varying NOT NULL,
    default_value character varying(512),
    format_pattern character varying(128),
    target_column character varying(128)
);


--
-- Name: TABLE sys_import_template_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_import_template_field IS '导入模板字段映射表';


--
-- Name: COLUMN sys_import_template_field.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.id IS '主键';


--
-- Name: COLUMN sys_import_template_field.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.create_by IS '创建人ID';


--
-- Name: COLUMN sys_import_template_field.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.create_time IS '创建时间';


--
-- Name: COLUMN sys_import_template_field.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.deleted IS '软删除标志';


--
-- Name: COLUMN sys_import_template_field.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.update_by IS '更新人ID';


--
-- Name: COLUMN sys_import_template_field.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.update_time IS '更新时间';


--
-- Name: COLUMN sys_import_template_field.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_import_template_field.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_import_template_field.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.enabled IS '是否启用';


--
-- Name: COLUMN sys_import_template_field.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.remark IS '备注';


--
-- Name: COLUMN sys_import_template_field.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.status IS '状态';


--
-- Name: COLUMN sys_import_template_field.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.sort_order IS '排序号';


--
-- Name: COLUMN sys_import_template_field.exact_match; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.exact_match IS '是否完全匹配';


--
-- Name: COLUMN sys_import_template_field.field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.field IS '目标字段';


--
-- Name: COLUMN sys_import_template_field.field_title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.field_title IS 'Excel列标题数组';


--
-- Name: COLUMN sys_import_template_field.multiple; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.multiple IS '是否多值字段';


--
-- Name: COLUMN sys_import_template_field.repeat; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.repeat IS '是否允许重复';


--
-- Name: COLUMN sys_import_template_field.required; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.required IS '是否必填字段';


--
-- Name: COLUMN sys_import_template_field.template_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.template_id IS '模板ID';


--
-- Name: COLUMN sys_import_template_field.converter_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.converter_key IS '自定义转换器键';


--
-- Name: COLUMN sys_import_template_field.data_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.data_type IS '数据类型';


--
-- Name: COLUMN sys_import_template_field.default_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.default_value IS '缺省值';


--
-- Name: COLUMN sys_import_template_field.format_pattern; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.format_pattern IS '日期时间等格式';


--
-- Name: COLUMN sys_import_template_field.target_column; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_import_template_field.target_column IS '数据库目标列';


--
-- Name: sys_login_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_login_log (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    client_ip character varying(50),
    location character varying(100),
    log_time timestamp(6) without time zone,
    message character varying(500),
    trace_id character varying(64),
    user_id character varying(64),
    username character varying(64),
    action character varying(50),
    browser character varying(100),
    os character varying(100),
    user_agent character varying(500)
);


--
-- Name: TABLE sys_login_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_login_log IS '登录日志表';


--
-- Name: COLUMN sys_login_log.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.id IS '主键';


--
-- Name: COLUMN sys_login_log.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.create_by IS '创建人ID';


--
-- Name: COLUMN sys_login_log.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.create_time IS '创建时间';


--
-- Name: COLUMN sys_login_log.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.deleted IS '软删除标志';


--
-- Name: COLUMN sys_login_log.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.update_by IS '更新人ID';


--
-- Name: COLUMN sys_login_log.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.update_time IS '更新时间';


--
-- Name: COLUMN sys_login_log.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_login_log.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_login_log.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.enabled IS '是否启用';


--
-- Name: COLUMN sys_login_log.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.remark IS '备注';


--
-- Name: COLUMN sys_login_log.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.status IS '状态';


--
-- Name: COLUMN sys_login_log.client_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.client_ip IS '客户端IP';


--
-- Name: COLUMN sys_login_log.location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.location IS '地理位置';


--
-- Name: COLUMN sys_login_log.log_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.log_time IS '日志发生时间';


--
-- Name: COLUMN sys_login_log.message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.message IS '日志消息';


--
-- Name: COLUMN sys_login_log.trace_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.trace_id IS '链路追踪ID';


--
-- Name: COLUMN sys_login_log.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.user_id IS '操作用户ID';


--
-- Name: COLUMN sys_login_log.username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.username IS '操作用户名';


--
-- Name: COLUMN sys_login_log.action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.action IS '登录动作';


--
-- Name: COLUMN sys_login_log.browser; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.browser IS '浏览器';


--
-- Name: COLUMN sys_login_log.os; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.os IS '操作系统';


--
-- Name: COLUMN sys_login_log.user_agent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.user_agent IS '客户端代理信息';


--
-- Name: sys_menu; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_menu (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    component character varying(255),
    icon character varying(128),
    is_external boolean,
    keep_alive boolean,
    menu_name character varying(128) NOT NULL,
    menu_title character varying(128),
    menu_type character varying(20) NOT NULL,
    parent_id character varying(64),
    path character varying(255),
    permission character varying(128),
    visible boolean,
    CONSTRAINT ck_sys_menu_type CHECK (menu_type IN ('DIRECTORY', 'MENU', 'BUTTON'))
);


--
-- Name: TABLE sys_menu; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_menu IS '菜单表';


--
-- Name: COLUMN sys_menu.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.id IS '主键';


--
-- Name: COLUMN sys_menu.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.create_by IS '创建人ID';


--
-- Name: COLUMN sys_menu.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.create_time IS '创建时间';


--
-- Name: COLUMN sys_menu.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.deleted IS '软删除标志';


--
-- Name: COLUMN sys_menu.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.update_by IS '更新人ID';


--
-- Name: COLUMN sys_menu.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.update_time IS '更新时间';


--
-- Name: COLUMN sys_menu.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_menu.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_menu.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.enabled IS '是否启用';


--
-- Name: COLUMN sys_menu.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.remark IS '备注';


--
-- Name: COLUMN sys_menu.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.status IS '状态';


--
-- Name: COLUMN sys_menu.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.sort_order IS '排序号';


--
-- Name: COLUMN sys_menu.component; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.component IS '前端组件注册键';


--
-- Name: COLUMN sys_menu.icon; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.icon IS '图标';


--
-- Name: COLUMN sys_menu.is_external; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.is_external IS '是否外链';


--
-- Name: COLUMN sys_menu.keep_alive; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.keep_alive IS '是否缓存';


--
-- Name: COLUMN sys_menu.menu_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.menu_name IS '菜单名称';


--
-- Name: COLUMN sys_menu.menu_title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.menu_title IS '菜单标题';


--
-- Name: COLUMN sys_menu.menu_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.menu_type IS '菜单类型：DIRECTORY-目录，MENU-页面菜单，BUTTON-按钮权限';


--
-- Name: COLUMN sys_menu.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.parent_id IS '父菜单ID';


--
-- Name: COLUMN sys_menu.path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.path IS '路由路径';


--
-- Name: COLUMN sys_menu.permission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.permission IS '权限标识';


--
-- Name: COLUMN sys_menu.visible; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.visible IS '是否可见';


--
-- Name: sys_message; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_message (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    business_id character varying(64),
    business_type character varying(64),
    display boolean NOT NULL,
    is_read boolean NOT NULL,
    msg_content character varying(5000) NOT NULL,
    msg_title character varying(256) NOT NULL,
    msg_type character varying(20) NOT NULL,
    priority character varying(20),
    read_time timestamp(6) without time zone,
    receiver_id character varying(64) NOT NULL,
    receiver_name character varying(128),
    sender_id character varying(64),
    sender_name character varying(128)
);


--
-- Name: TABLE sys_message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_message IS '系统消息表';


--
-- Name: COLUMN sys_message.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.id IS '主键';


--
-- Name: COLUMN sys_message.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.create_by IS '创建人ID';


--
-- Name: COLUMN sys_message.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.create_time IS '创建时间';


--
-- Name: COLUMN sys_message.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.deleted IS '软删除标志';


--
-- Name: COLUMN sys_message.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.update_by IS '更新人ID';


--
-- Name: COLUMN sys_message.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.update_time IS '更新时间';


--
-- Name: COLUMN sys_message.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_message.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_message.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.enabled IS '是否启用';


--
-- Name: COLUMN sys_message.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.remark IS '备注';


--
-- Name: COLUMN sys_message.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.status IS '状态';


--
-- Name: COLUMN sys_message.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.sort_order IS '排序号';


--
-- Name: COLUMN sys_message.business_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.business_id IS '关联业务ID';


--
-- Name: COLUMN sys_message.business_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.business_type IS '关联业务类型';


--
-- Name: COLUMN sys_message.display; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.display IS '是否展示在公告栏';


--
-- Name: COLUMN sys_message.is_read; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.is_read IS '是否已读';


--
-- Name: COLUMN sys_message.msg_content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.msg_content IS '消息内容';


--
-- Name: COLUMN sys_message.msg_title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.msg_title IS '消息标题';


--
-- Name: COLUMN sys_message.msg_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.msg_type IS '消息类型(1-系统公告,2-普通消息,3-工作提醒,4-审批通知)';


--
-- Name: COLUMN sys_message.priority; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.priority IS '优先级(1-高,2-中,3-低)';


--
-- Name: COLUMN sys_message.read_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.read_time IS '读取时间';


--
-- Name: COLUMN sys_message.receiver_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.receiver_id IS '接收人ID';


--
-- Name: COLUMN sys_message.receiver_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.receiver_name IS '接收人姓名';


--
-- Name: COLUMN sys_message.sender_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.sender_id IS '发送人ID';


--
-- Name: COLUMN sys_message.sender_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_message.sender_name IS '发送人姓名';


--
-- Name: sys_operation_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_operation_log (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    client_ip character varying(50),
    location character varying(100),
    log_time timestamp(6) without time zone,
    message character varying(500),
    trace_id character varying(64),
    user_id character varying(64),
    username character varying(64),
    cost_ms bigint,
    description character varying(500),
    error_message character varying(2000),
    module character varying(100),
    operation_id character varying(64) NOT NULL,
    operation_type character varying(50),
    request_method character varying(10),
    request_params text,
    request_url character varying(500),
    response_data text,
    user_agent character varying(500)
);


--
-- Name: TABLE sys_operation_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_operation_log IS '操作日志表';


--
-- Name: COLUMN sys_operation_log.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.id IS '主键';


--
-- Name: COLUMN sys_operation_log.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.create_by IS '创建人ID';


--
-- Name: COLUMN sys_operation_log.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.create_time IS '创建时间';


--
-- Name: COLUMN sys_operation_log.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.deleted IS '软删除标志';


--
-- Name: COLUMN sys_operation_log.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.update_by IS '更新人ID';


--
-- Name: COLUMN sys_operation_log.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.update_time IS '更新时间';


--
-- Name: COLUMN sys_operation_log.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_operation_log.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_operation_log.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.enabled IS '是否启用';


--
-- Name: COLUMN sys_operation_log.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.remark IS '备注';


--
-- Name: COLUMN sys_operation_log.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.status IS '状态';


--
-- Name: COLUMN sys_operation_log.client_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.client_ip IS '客户端IP';


--
-- Name: COLUMN sys_operation_log.location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.location IS '地理位置';


--
-- Name: COLUMN sys_operation_log.log_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.log_time IS '日志发生时间';


--
-- Name: COLUMN sys_operation_log.message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.message IS '日志消息';


--
-- Name: COLUMN sys_operation_log.trace_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.trace_id IS '链路追踪ID';


--
-- Name: COLUMN sys_operation_log.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.user_id IS '操作用户ID';


--
-- Name: COLUMN sys_operation_log.username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.username IS '操作用户名';


--
-- Name: COLUMN sys_operation_log.cost_ms; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.cost_ms IS '执行耗时(ms)';


--
-- Name: COLUMN sys_operation_log.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.description IS '操作描述';


--
-- Name: COLUMN sys_operation_log.error_message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.error_message IS '错误信息';


--
-- Name: COLUMN sys_operation_log.module; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.module IS '所属模块';


--
-- Name: COLUMN sys_operation_log.operation_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.operation_id IS '操作ID(UUID)';


--
-- Name: COLUMN sys_operation_log.operation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.operation_type IS '操作类型';


--
-- Name: COLUMN sys_operation_log.request_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.request_method IS 'HTTP请求方法';


--
-- Name: COLUMN sys_operation_log.request_params; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.request_params IS '请求参数JSON';


--
-- Name: COLUMN sys_operation_log.request_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.request_url IS '请求URL';


--
-- Name: COLUMN sys_operation_log.response_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.response_data IS '响应结果JSON';


--
-- Name: COLUMN sys_operation_log.user_agent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_operation_log.user_agent IS '客户端代理信息';


--
-- Name: sys_oss_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_oss_log (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    client_ip character varying(50),
    location character varying(100),
    log_time timestamp(6) without time zone,
    message character varying(500),
    trace_id character varying(64),
    user_id character varying(64),
    username character varying(64),
    biz_module character varying(50),
    bucket_name character varying(128),
    content_type character varying(128),
    file_size bigint,
    file_suffix character varying(32),
    file_url character varying(1024),
    object_key character varying(512),
    original_name character varying(256),
    file_hash character varying(64),
    file_md5 character varying(64),
    stored_name character varying(256),
    upload_status boolean,
    upload_time timestamp(6) without time zone
);


--
-- Name: TABLE sys_oss_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_oss_log IS 'OSS文件上传日志表';


--
-- Name: COLUMN sys_oss_log.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.id IS '主键';


--
-- Name: COLUMN sys_oss_log.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.create_by IS '创建人ID';


--
-- Name: COLUMN sys_oss_log.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.create_time IS '创建时间';


--
-- Name: COLUMN sys_oss_log.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.deleted IS '软删除标志';


--
-- Name: COLUMN sys_oss_log.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.update_by IS '更新人ID';


--
-- Name: COLUMN sys_oss_log.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.update_time IS '更新时间';


--
-- Name: COLUMN sys_oss_log.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_oss_log.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_oss_log.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.enabled IS '是否启用';


--
-- Name: COLUMN sys_oss_log.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.remark IS '备注';


--
-- Name: COLUMN sys_oss_log.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.status IS '状态';


--
-- Name: COLUMN sys_oss_log.client_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.client_ip IS '客户端IP';


--
-- Name: COLUMN sys_oss_log.location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.location IS '地理位置';


--
-- Name: COLUMN sys_oss_log.log_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.log_time IS '日志发生时间';


--
-- Name: COLUMN sys_oss_log.message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.message IS '日志消息';


--
-- Name: COLUMN sys_oss_log.trace_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.trace_id IS '链路追踪ID';


--
-- Name: COLUMN sys_oss_log.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.user_id IS '操作用户ID';


--
-- Name: COLUMN sys_oss_log.username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.username IS '操作用户名';


--
-- Name: COLUMN sys_oss_log.biz_module; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.biz_module IS '业务模块标识';


--
-- Name: COLUMN sys_oss_log.bucket_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.bucket_name IS '存储桶名称';


--
-- Name: COLUMN sys_oss_log.content_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.content_type IS 'MIME类型';


--
-- Name: COLUMN sys_oss_log.file_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.file_size IS '文件大小(字节)';


--
-- Name: COLUMN sys_oss_log.file_suffix; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.file_suffix IS '文件后缀';


--
-- Name: COLUMN sys_oss_log.file_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.file_url IS '文件访问URL';


--
-- Name: COLUMN sys_oss_log.object_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.object_key IS 'S3对象键';


--
-- Name: COLUMN sys_oss_log.original_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.original_name IS '原始文件名';


--
-- Name: COLUMN sys_oss_log.file_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_oss_log.file_hash IS '文件SHA-256';


--
-- Name: sys_post; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_post (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    data_scope character varying(32),
    dept_id character varying(64),
    parent_id character varying(64),
    post_code character varying(50),
    post_name character varying(50) NOT NULL,
    CONSTRAINT sys_post_data_scope_check CHECK (((data_scope)::text = ANY ((ARRAY['ALL'::character varying, 'DEPT'::character varying, 'DEPT_AND_CHILD'::character varying, 'SELF'::character varying, 'CUSTOM'::character varying])::text[])))
);


--
-- Name: TABLE sys_post; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_post IS '岗位表';


--
-- Name: COLUMN sys_post.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.id IS '主键';


--
-- Name: COLUMN sys_post.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.create_by IS '创建人ID';


--
-- Name: COLUMN sys_post.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.create_time IS '创建时间';


--
-- Name: COLUMN sys_post.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.deleted IS '软删除标志';


--
-- Name: COLUMN sys_post.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.update_by IS '更新人ID';


--
-- Name: COLUMN sys_post.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.update_time IS '更新时间';


--
-- Name: COLUMN sys_post.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_post.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_post.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.enabled IS '是否启用';


--
-- Name: COLUMN sys_post.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.remark IS '备注';


--
-- Name: COLUMN sys_post.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.status IS '状态';


--
-- Name: COLUMN sys_post.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.sort_order IS '排序号';


--
-- Name: COLUMN sys_post.data_scope; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.data_scope IS '数据权限范围';


--
-- Name: COLUMN sys_post.dept_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.dept_id IS '部门ID';


--
-- Name: COLUMN sys_post.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.parent_id IS '上级岗位ID';


--
-- Name: COLUMN sys_post.post_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.post_code IS '岗位编码';


--
-- Name: COLUMN sys_post.post_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_post.post_name IS '岗位名称';


--
-- Name: sys_region; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_region (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    full_name character varying(500),
    latitude numeric(10,6),
    longitude numeric(10,6),
    parent_id character varying(64),
    region_code character varying(20) NOT NULL,
    region_level character varying(64) NOT NULL,
    region_name character varying(100) NOT NULL,
    short_name character varying(50),
    sort_order integer,
    zip_code character varying(10)
);


--
-- Name: TABLE sys_region; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_region IS '行政区域表';


--
-- Name: COLUMN sys_region.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.id IS '主键';


--
-- Name: COLUMN sys_region.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.create_by IS '创建人ID';


--
-- Name: COLUMN sys_region.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.create_time IS '创建时间';


--
-- Name: COLUMN sys_region.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.deleted IS '软删除标志';


--
-- Name: COLUMN sys_region.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.update_by IS '更新人ID';


--
-- Name: COLUMN sys_region.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.update_time IS '更新时间';


--
-- Name: COLUMN sys_region.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_region.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.enabled IS '是否启用';


--
-- Name: COLUMN sys_region.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.remark IS '备注';


--
-- Name: COLUMN sys_region.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.status IS '状态';




--
-- Name: COLUMN sys_region.full_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.full_name IS '完整路径名称';




--
-- Name: COLUMN sys_region.latitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.latitude IS '纬度';


--
-- Name: COLUMN sys_region.longitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.longitude IS '经度';


--
-- Name: COLUMN sys_region.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.parent_id IS '父区域ID';


--
-- Name: COLUMN sys_region.region_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.region_code IS '区域编码';


--
-- Name: COLUMN sys_region.region_level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.region_level IS '层级';


--
-- Name: COLUMN sys_region.region_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.region_name IS '区域名称';


--
-- Name: COLUMN sys_region.short_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.short_name IS '区域简称';


--
-- Name: COLUMN sys_region.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.sort_order IS '排序号';


--
-- Name: COLUMN sys_region.zip_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_region.zip_code IS '邮政编码';


--
-- Name: sys_register; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_register (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    audit_by character varying(64),
    audit_remark character varying(500),
    audit_time timestamp(6) without time zone,
    dept_id character varying(64) NOT NULL,
    email character varying(128),
    gender character varying(20),
    nickname character varying(64),
    password character varying(255) NOT NULL,
    personnel_id character varying(64) NOT NULL,
    provisioning_error character varying(500),
    registration_state character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    phone character varying(11),
    real_name character varying(64) NOT NULL,
    user_id character varying(64),
    username character varying(64),
    CONSTRAINT ck_sys_register_state CHECK ((registration_state)::text = ANY ((ARRAY['PENDING'::character varying, 'REJECTED'::character varying, 'PROVISIONING'::character varying, 'PROVISIONING_FAILED'::character varying, 'ACTIVE'::character varying])::text[]))
);

CREATE TABLE public.sys_outbox (
    id character varying(64) NOT NULL,
    create_by character varying(64), create_time timestamp(6) without time zone,
    deleted boolean NOT NULL, update_by character varying(64), update_time timestamp(6) without time zone,
    version integer, tenant_id character varying(64) NOT NULL, enabled boolean, remark character varying(500),
    status character varying(20), sort_order integer,
    aggregate_id character varying(64) NOT NULL, event_type character varying(64) NOT NULL CHECK (event_type = 'REGISTER_PROVISION'),
    delivery_state character varying(20) NOT NULL, attempt_count integer DEFAULT 0 NOT NULL,
    available_at timestamp(6) without time zone NOT NULL, last_error character varying(500)
);


--
-- Name: TABLE sys_register; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_register IS '用户注册记录表';


--
-- Name: COLUMN sys_register.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.id IS '主键';


--
-- Name: COLUMN sys_register.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.create_by IS '创建人ID';


--
-- Name: COLUMN sys_register.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.create_time IS '创建时间';


--
-- Name: COLUMN sys_register.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.deleted IS '软删除标志';


--
-- Name: COLUMN sys_register.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.update_by IS '更新人ID';


--
-- Name: COLUMN sys_register.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.update_time IS '更新时间';


--
-- Name: COLUMN sys_register.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_register.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_register.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.enabled IS '是否启用';


--
-- Name: COLUMN sys_register.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.remark IS '备注';


--
-- Name: COLUMN sys_register.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.status IS '状态';


--
-- Name: COLUMN sys_register.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.sort_order IS '排序号';


--
-- Name: COLUMN sys_register.audit_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.audit_by IS '审核人ID';


--
-- Name: COLUMN sys_register.audit_remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.audit_remark IS '审核备注';


--
-- Name: COLUMN sys_register.audit_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.audit_time IS '审核时间';


--
-- Name: COLUMN sys_register.dept_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.dept_id IS '注册部门ID';


--
-- Name: COLUMN sys_register.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.email IS '邮箱';


--
-- Name: COLUMN sys_register.gender; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.gender IS '性别(0-未知,1-男,2-女)';


--
-- Name: COLUMN sys_register.nickname; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.nickname IS '昵称';


--
-- Name: COLUMN sys_register.password; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.password IS '密码(加密存储)';


--
-- Name: COLUMN sys_register.personnel_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.personnel_id IS '匹配员工档案ID';


--
-- Name: COLUMN sys_register.provisioning_error; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.provisioning_error IS '账户开通失败原因';


--
-- Name: COLUMN sys_register.registration_state; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.registration_state IS '注册开通状态';


--
-- Name: COLUMN sys_register.phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.phone IS '手机号';


--
-- Name: COLUMN sys_register.real_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.real_name IS '真实姓名';


--
-- Name: COLUMN sys_register.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.user_id IS '关联平台用户ID';


--
-- Name: COLUMN sys_register.username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_register.username IS '用户名(系统根据真实姓名自动生成)';


COMMENT ON TABLE public.sys_outbox IS '系统可靠出站任务表';
COMMENT ON COLUMN public.sys_outbox.id IS '主键';
COMMENT ON COLUMN public.sys_outbox.create_by IS '创建人ID';
COMMENT ON COLUMN public.sys_outbox.create_time IS '创建时间';
COMMENT ON COLUMN public.sys_outbox.deleted IS '软删除标志';
COMMENT ON COLUMN public.sys_outbox.update_by IS '更新人ID';
COMMENT ON COLUMN public.sys_outbox.update_time IS '更新时间';
COMMENT ON COLUMN public.sys_outbox.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_outbox.tenant_id IS '租户ID';
COMMENT ON COLUMN public.sys_outbox.enabled IS '是否启用';
COMMENT ON COLUMN public.sys_outbox.remark IS '备注';
COMMENT ON COLUMN public.sys_outbox.status IS '状态';
COMMENT ON COLUMN public.sys_outbox.sort_order IS '排序号';
COMMENT ON COLUMN public.sys_outbox.aggregate_id IS '注册记录ID';
COMMENT ON COLUMN public.sys_outbox.event_type IS '事件类型';
COMMENT ON COLUMN public.sys_outbox.delivery_state IS '投递状态';
COMMENT ON COLUMN public.sys_outbox.attempt_count IS '已尝试次数';
COMMENT ON COLUMN public.sys_outbox.available_at IS '下次可处理时间';
COMMENT ON COLUMN public.sys_outbox.last_error IS '最近失败原因';


--
-- Name: sys_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_role (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    data_scope integer,
    default_registration_role boolean DEFAULT false NOT NULL,
    role_code character varying(64) NOT NULL,
    role_name character varying(128) NOT NULL
);


--
-- Name: TABLE sys_role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_role IS '角色表';


--
-- Name: COLUMN sys_role.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.id IS '主键';


--
-- Name: COLUMN sys_role.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.create_by IS '创建人ID';


--
-- Name: COLUMN sys_role.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.create_time IS '创建时间';


--
-- Name: COLUMN sys_role.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.deleted IS '软删除标志';


--
-- Name: COLUMN sys_role.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.update_by IS '更新人ID';


--
-- Name: COLUMN sys_role.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.update_time IS '更新时间';


--
-- Name: COLUMN sys_role.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_role.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_role.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.enabled IS '是否启用';


--
-- Name: COLUMN sys_role.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.remark IS '备注';


--
-- Name: COLUMN sys_role.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.status IS '状态';


--
-- Name: COLUMN sys_role.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.sort_order IS '排序号';


--
-- Name: COLUMN sys_role.data_scope; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.data_scope IS '数据权限范围';


--
-- Name: COLUMN sys_role.default_registration_role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.default_registration_role IS '是否为默认注册角色(注册审核兜底使用)';


--
-- Name: COLUMN sys_role.role_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.role_code IS '角色编码';


--
-- Name: COLUMN sys_role.role_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.role_name IS '角色名称';


--
-- Name: sys_role_menu; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_role_menu (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    menu_id character varying(64) NOT NULL,
    role_id character varying(64) NOT NULL
);


--
-- Name: TABLE sys_role_menu; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_role_menu IS '角色菜单关系表';


--
-- Name: COLUMN sys_role_menu.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.id IS '主键';


--
-- Name: COLUMN sys_role_menu.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.create_by IS '创建人ID';


--
-- Name: COLUMN sys_role_menu.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.create_time IS '创建时间';


--
-- Name: COLUMN sys_role_menu.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.deleted IS '软删除标志';


--
-- Name: COLUMN sys_role_menu.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.update_by IS '更新人ID';


--
-- Name: COLUMN sys_role_menu.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.update_time IS '更新时间';


--
-- Name: COLUMN sys_role_menu.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_role_menu.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_role_menu.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.enabled IS '是否启用';


--
-- Name: COLUMN sys_role_menu.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.remark IS '备注';


--
-- Name: COLUMN sys_role_menu.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.status IS '状态';


--
-- Name: COLUMN sys_role_menu.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.sort_order IS '排序号';


--
-- Name: COLUMN sys_role_menu.menu_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.menu_id IS '菜单ID';


--
-- Name: COLUMN sys_role_menu.role_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.role_id IS '角色ID';


--
-- Name: sys_setting; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_setting (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    extensions jsonb NOT NULL DEFAULT '{}'::jsonb,
    auto_save boolean,
    desktop_notification boolean,
    email_notification boolean,
    font_size integer,
    language character varying(32),
    login_protection boolean,
    notification_frequency character varying(32),
    owner_id character varying(64),
    session_timeout integer,
    sound_notification boolean,
    theme character varying(32)
);


--
-- Name: TABLE sys_setting; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_setting IS '应用设置表';


--
-- Name: COLUMN sys_setting.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.id IS '主键';


--
-- Name: COLUMN sys_setting.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.create_by IS '创建人ID';


--
-- Name: COLUMN sys_setting.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.create_time IS '创建时间';


--
-- Name: COLUMN sys_setting.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.deleted IS '软删除标志';


--
-- Name: COLUMN sys_setting.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.update_by IS '更新人ID';


--
-- Name: COLUMN sys_setting.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.update_time IS '更新时间';


--
-- Name: COLUMN sys_setting.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_setting.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_setting.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.enabled IS '是否启用';


--
-- Name: COLUMN sys_setting.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.remark IS '备注';


--
-- Name: COLUMN sys_setting.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.status IS '状态';


--
-- Name: COLUMN sys_setting.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.sort_order IS '排序号';


--
-- Name: COLUMN sys_setting.auto_save; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.auto_save IS '自动保存';


















--
-- Name: COLUMN sys_setting.desktop_notification; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.desktop_notification IS '桌面通知';


--
-- Name: COLUMN sys_setting.email_notification; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.email_notification IS '邮件通知';




--
-- Name: COLUMN sys_setting.font_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.font_size IS '字体大小';


--
-- Name: COLUMN sys_setting.language; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.language IS '语言';


--
-- Name: COLUMN sys_setting.login_protection; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.login_protection IS '登录保护';


--
-- Name: COLUMN sys_setting.notification_frequency; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.notification_frequency IS '通知频率';


--
-- Name: COLUMN sys_setting.owner_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.owner_id IS '所属人ID';


--
-- Name: COLUMN sys_setting.session_timeout; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.session_timeout IS '会话超时(分钟)';


--
-- Name: COLUMN sys_setting.sound_notification; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.sound_notification IS '声音提示';


--
-- Name: COLUMN sys_setting.theme; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.theme IS '主题';


--
-- Name: sys_tenant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_tenant (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    address character varying(255),
    contact_email character varying(128),
    contact_name character varying(64),
    contact_phone character varying(20),
    description character varying(500),
    domain character varying(128),
    expire_time timestamp(6) without time zone,
    logo character varying(500),
    sort_order integer,
    tenant_code character varying(64),
    tenant_name character varying(128) NOT NULL,
    user_limit integer
);


--
-- Name: TABLE sys_tenant; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_tenant IS '租户表';


--
-- Name: COLUMN sys_tenant.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.id IS '主键';


--
-- Name: COLUMN sys_tenant.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.create_by IS '创建人ID';


--
-- Name: COLUMN sys_tenant.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.create_time IS '创建时间';


--
-- Name: COLUMN sys_tenant.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.deleted IS '软删除标志';


--
-- Name: COLUMN sys_tenant.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.update_by IS '更新人ID';


--
-- Name: COLUMN sys_tenant.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.update_time IS '更新时间';


--
-- Name: COLUMN sys_tenant.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_tenant.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.enabled IS '是否启用';


--
-- Name: COLUMN sys_tenant.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.remark IS '备注';


--
-- Name: COLUMN sys_tenant.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.status IS '状态';


--
-- Name: COLUMN sys_tenant.address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.address IS '租户地址';


--
-- Name: COLUMN sys_tenant.contact_email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.contact_email IS '联系邮箱';


--
-- Name: COLUMN sys_tenant.contact_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.contact_name IS '联系人';


--
-- Name: COLUMN sys_tenant.contact_phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.contact_phone IS '联系电话';


--
-- Name: COLUMN sys_tenant.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.description IS '租户描述';


--
-- Name: COLUMN sys_tenant.domain; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.domain IS '租户域名';


--
-- Name: COLUMN sys_tenant.expire_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.expire_time IS '过期时间';


--
-- Name: COLUMN sys_tenant.logo; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.logo IS '租户Logo URL';


--
-- Name: COLUMN sys_tenant.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.sort_order IS '排序号';


--
-- Name: COLUMN sys_tenant.tenant_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.tenant_code IS '租户编码';


--
-- Name: COLUMN sys_tenant.tenant_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.tenant_name IS '租户名称';


--
-- Name: COLUMN sys_tenant.user_limit; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_tenant.user_limit IS '用户数量限制';


--
-- Name: sys_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    avatar character varying(500),
    dept_id character varying(64),
    email character varying(128),
    gender integer,
    last_login_ip character varying(17),
    last_login_time timestamp(6) without time zone,
    nickname character varying(64),
    password character varying(255) NOT NULL,
    phone character varying(11),
    real_name character varying(64),
    user_type character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    username character varying(64) NOT NULL
);


--
-- Name: TABLE sys_user; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_user IS '用户表';


--
-- Name: COLUMN sys_user.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.id IS '主键';


--
-- Name: COLUMN sys_user.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.create_by IS '创建人ID';


--
-- Name: COLUMN sys_user.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.create_time IS '创建时间';


--
-- Name: COLUMN sys_user.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.deleted IS '软删除标志';


--
-- Name: COLUMN sys_user.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.update_by IS '更新人ID';


--
-- Name: COLUMN sys_user.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.update_time IS '更新时间';


--
-- Name: COLUMN sys_user.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_user.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_user.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.enabled IS '是否启用';


--
-- Name: COLUMN sys_user.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.remark IS '备注';


--
-- Name: COLUMN sys_user.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.status IS '状态';


--
-- Name: COLUMN sys_user.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.sort_order IS '排序号';


--
-- Name: COLUMN sys_user.avatar; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.avatar IS '头像URL';


--
-- Name: COLUMN sys_user.dept_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.dept_id IS '部门ID';


--
-- Name: COLUMN sys_user.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.email IS '邮箱';


--
-- Name: COLUMN sys_user.gender; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.gender IS '性别(0-未知,1-男,2-女)';


--
-- Name: COLUMN sys_user.last_login_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.last_login_ip IS '最后登录IP';


--
-- Name: COLUMN sys_user.last_login_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.last_login_time IS '最后登录时间';


--
-- Name: COLUMN sys_user.nickname; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.nickname IS '昵称';


--
-- Name: COLUMN sys_user.password; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.password IS '密码';


--
-- Name: COLUMN sys_user.phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.phone IS '手机号';


--
-- Name: COLUMN sys_user.real_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.real_name IS '真实姓名';


--
--
-- Name: COLUMN sys_user.user_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.user_type IS '用户类型(ADMIN-管理员,USER-用户)';


--
-- Name: COLUMN sys_user.username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.username IS '用户名';


--
-- Name: sys_user_post; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user_post (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    end_date timestamp(6) without time zone,
    main_post boolean,
    post_id character varying(64) NOT NULL,
    post_type character varying(32),
    start_date timestamp(6) without time zone,
    user_id character varying(64) NOT NULL
);


--
-- Name: TABLE sys_user_post; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_user_post IS '用户岗位关系表';


--
-- Name: COLUMN sys_user_post.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.id IS '主键';


--
-- Name: COLUMN sys_user_post.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.create_by IS '创建人ID';


--
-- Name: COLUMN sys_user_post.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.create_time IS '创建时间';


--
-- Name: COLUMN sys_user_post.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.deleted IS '软删除标志';


--
-- Name: COLUMN sys_user_post.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.update_by IS '更新人ID';


--
-- Name: COLUMN sys_user_post.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.update_time IS '更新时间';


--
-- Name: COLUMN sys_user_post.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_user_post.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_user_post.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.enabled IS '是否启用';


--
-- Name: COLUMN sys_user_post.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.remark IS '备注';


--
-- Name: COLUMN sys_user_post.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.status IS '状态';


--
-- Name: COLUMN sys_user_post.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.sort_order IS '排序号';


--
-- Name: COLUMN sys_user_post.end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.end_date IS '任职结束日期（为空表示当前有效）';


--
-- Name: COLUMN sys_user_post.main_post; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.main_post IS '是否主岗：true/false';


--
-- Name: COLUMN sys_user_post.post_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.post_id IS '岗位ID';


--
-- Name: COLUMN sys_user_post.post_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.post_type IS '岗位类型：借调岗、临时岗等';


--
-- Name: COLUMN sys_user_post.start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.start_date IS '任职开始日期';


--
-- Name: COLUMN sys_user_post.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_post.user_id IS '用户ID';


--
-- Name: sys_user_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user_role (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    role_id character varying(64) NOT NULL,
    user_id character varying(64) NOT NULL
);


--
-- Name: TABLE sys_user_role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_user_role IS '用户角色关系表';


--
-- Name: COLUMN sys_user_role.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.id IS '主键';


--
-- Name: COLUMN sys_user_role.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.create_by IS '创建人ID';


--
-- Name: COLUMN sys_user_role.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.create_time IS '创建时间';


--
-- Name: COLUMN sys_user_role.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.deleted IS '软删除标志';


--
-- Name: COLUMN sys_user_role.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.update_by IS '更新人ID';


--
-- Name: COLUMN sys_user_role.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.update_time IS '更新时间';


--
-- Name: COLUMN sys_user_role.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_user_role.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_user_role.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.enabled IS '是否启用';


--
-- Name: COLUMN sys_user_role.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.remark IS '备注';


--
-- Name: COLUMN sys_user_role.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.status IS '状态';


--
-- Name: COLUMN sys_user_role.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.sort_order IS '排序号';


--
-- Name: COLUMN sys_user_role.role_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.role_id IS '角色ID';


--
-- Name: COLUMN sys_user_role.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.user_id IS '用户ID';


--
-- Name: sys_weather; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_weather (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id character varying(64) NOT NULL,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    sort_order integer,
    aqi integer,
    aqi_level character varying(20),
    city_code character varying(20) NOT NULL,
    city_name character varying(50) NOT NULL,
    collect_time timestamp(6) without time zone,
    data_source character varying(50),
    humidity character varying(10),
    province character varying(50),
    temp_high numeric(5,1),
    temp_low numeric(5,1),
    temperature numeric(5,1),
    weather_condition character varying(50),
    weather_date date NOT NULL,
    weather_icon character varying(50),
    wind_direction character varying(20),
    wind_power character varying(20)
);


--
-- Name: TABLE sys_weather; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_weather IS '天气信息表';


--
-- Name: COLUMN sys_weather.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.id IS '主键';


--
-- Name: COLUMN sys_weather.create_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.create_by IS '创建人ID';


--
-- Name: COLUMN sys_weather.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.create_time IS '创建时间';


--
-- Name: COLUMN sys_weather.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.deleted IS '软删除标志';


--
-- Name: COLUMN sys_weather.update_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.update_by IS '更新人ID';


--
-- Name: COLUMN sys_weather.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.update_time IS '更新时间';


--
-- Name: COLUMN sys_weather.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.version IS '乐观锁版本号';


--
-- Name: COLUMN sys_weather.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.tenant_id IS '租户ID';


--
-- Name: COLUMN sys_weather.enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.enabled IS '是否启用';


--
-- Name: COLUMN sys_weather.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.remark IS '备注';


--
-- Name: COLUMN sys_weather.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.status IS '状态';


--
-- Name: COLUMN sys_weather.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.sort_order IS '排序号';


--
-- Name: COLUMN sys_weather.aqi; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.aqi IS '空气质量指数';


--
-- Name: COLUMN sys_weather.aqi_level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.aqi_level IS '空气质量等级';


--
-- Name: COLUMN sys_weather.city_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.city_code IS '城市编码';


--
-- Name: COLUMN sys_weather.city_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.city_name IS '城市名称';


--
-- Name: COLUMN sys_weather.collect_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.collect_time IS '数据采集时间';


--
-- Name: COLUMN sys_weather.data_source; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.data_source IS '数据来源';


--
-- Name: COLUMN sys_weather.humidity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.humidity IS '湿度(%)';


--
-- Name: COLUMN sys_weather.province; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.province IS '省份';


--
-- Name: COLUMN sys_weather.temp_high; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.temp_high IS '最高温度(℃)';


--
-- Name: COLUMN sys_weather.temp_low; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.temp_low IS '最低温度(℃)';


--
-- Name: COLUMN sys_weather.temperature; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.temperature IS '当前温度(℃)';


--
-- Name: COLUMN sys_weather.weather_condition; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.weather_condition IS '天气状况';


--
-- Name: COLUMN sys_weather.weather_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.weather_date IS '天气日期';


--
-- Name: COLUMN sys_weather.weather_icon; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.weather_icon IS '天气图标编码';


--
-- Name: COLUMN sys_weather.wind_direction; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.wind_direction IS '风向';


--
-- Name: COLUMN sys_weather.wind_power; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_weather.wind_power IS '风力等级';


--
-- Name: sys_calendar sys_calendar_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_calendar
    ADD CONSTRAINT sys_calendar_pkey PRIMARY KEY (id);


--
-- Name: sys_dept sys_dept_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dept
    ADD CONSTRAINT sys_dept_pkey PRIMARY KEY (id);


--
-- Name: sys_dict_item sys_dict_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict_item
    ADD CONSTRAINT sys_dict_item_pkey PRIMARY KEY (id);


--
-- Name: sys_dict sys_dict_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict
    ADD CONSTRAINT sys_dict_pkey PRIMARY KEY (id);


--
-- Name: sys_import_template_field sys_import_template_field_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_import_template_field
    ADD CONSTRAINT sys_import_template_field_pkey PRIMARY KEY (id);


--
-- Name: sys_import_template sys_import_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_import_template
    ADD CONSTRAINT sys_import_template_pkey PRIMARY KEY (id);


--
-- Name: sys_login_log sys_login_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_login_log
    ADD CONSTRAINT sys_login_log_pkey PRIMARY KEY (id);


--
-- Name: sys_menu sys_menu_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_menu
    ADD CONSTRAINT sys_menu_pkey PRIMARY KEY (id);


--
-- Name: sys_message sys_message_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_message
    ADD CONSTRAINT sys_message_pkey PRIMARY KEY (id);


--
-- Name: sys_operation_log sys_operation_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_operation_log
    ADD CONSTRAINT sys_operation_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_operation_log
    ADD CONSTRAINT uk_sys_operation_log_operation_id UNIQUE (operation_id);


--
-- Name: sys_oss_log sys_oss_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_oss_log
    ADD CONSTRAINT sys_oss_log_pkey PRIMARY KEY (id);


--
-- Name: sys_post sys_post_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_post
    ADD CONSTRAINT sys_post_pkey PRIMARY KEY (id);


--
-- Name: sys_region sys_region_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_region
    ADD CONSTRAINT sys_region_pkey PRIMARY KEY (id);


--
-- Name: sys_register sys_register_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_register
    ADD CONSTRAINT sys_register_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_outbox
    ADD CONSTRAINT sys_outbox_pkey PRIMARY KEY (id);


--
-- Name: sys_role_menu sys_role_menu_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_menu
    ADD CONSTRAINT sys_role_menu_pkey PRIMARY KEY (id);


--
-- Name: sys_role sys_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_pkey PRIMARY KEY (id);


--
-- Name: sys_setting sys_setting_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_setting
    ADD CONSTRAINT sys_setting_pkey PRIMARY KEY (id);


--
-- Name: sys_tenant sys_tenant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_tenant
    ADD CONSTRAINT sys_tenant_pkey PRIMARY KEY (id);


--
-- Name: sys_user sys_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_pkey PRIMARY KEY (id);


--
-- Name: sys_user_post sys_user_post_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_post
    ADD CONSTRAINT sys_user_post_pkey PRIMARY KEY (id);


--
-- Name: sys_user_role sys_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_pkey PRIMARY KEY (id);


--
-- Name: sys_weather sys_weather_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_weather
    ADD CONSTRAINT sys_weather_pkey PRIMARY KEY (id);


--
-- Name: sys_role_menu uk_role_menu_tenant; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_menu
    ADD CONSTRAINT uk_role_menu_tenant UNIQUE (role_id, menu_id, tenant_id);


--
-- Name: sys_calendar uk_sys_calendar_year; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_calendar
    ADD CONSTRAINT uk_sys_calendar_year UNIQUE (year);


--
-- Name: sys_region uk_sys_region_code_retained; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_region
    ADD CONSTRAINT uk_sys_region_code_retained UNIQUE (region_code);




--
-- Name: sys_user_post uk_user_post_tenant; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_post
    ADD CONSTRAINT uk_user_post_tenant UNIQUE (user_id, post_id, tenant_id);


--
-- Name: sys_user_role uk_user_role_tenant; Type: CONSTRAINT; Schema: public; Owner: -
--

--
-- Name: idx_login_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_login_log_time ON public.sys_login_log USING btree (log_time);


--
-- Name: idx_login_log_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_login_log_username ON public.sys_login_log USING btree (username);


--
-- Name: idx_oper_log_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oper_log_module ON public.sys_operation_log USING btree (module);


--
-- Name: idx_oper_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oper_log_time ON public.sys_operation_log USING btree (log_time);


--
-- Name: idx_oper_log_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oper_log_username ON public.sys_operation_log USING btree (username);


--
-- Name: idx_oss_log_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oss_log_module ON public.sys_oss_log USING btree (biz_module);


--
-- Name: idx_oss_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oss_log_time ON public.sys_oss_log USING btree (log_time);


--
-- Name: idx_oss_log_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oss_log_user ON public.sys_oss_log USING btree (user_id);


--
-- Name: uk_sys_calendar_year_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_calendar_year_active ON public.sys_calendar USING btree (year) WHERE (deleted = false);


--
-- Name: uk_sys_post_code_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_post_code_active ON public.sys_post USING btree (tenant_id, dept_id, post_code) NULLS NOT DISTINCT WHERE (deleted = false);


--
-- Name: uk_sys_role_code_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_role_code_active ON public.sys_role USING btree (tenant_id, role_code) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_sys_role_default_registration_active ON public.sys_role USING btree (tenant_id) WHERE ((deleted = false) AND (default_registration_role = true));


--
-- Name: uk_sys_role_menu_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_role_menu_active ON public.sys_role_menu USING btree (tenant_id, role_id, menu_id) WHERE (deleted = false);


--
-- Name: uk_sys_menu_name_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_menu_name_active ON public.sys_menu USING btree (tenant_id, menu_name) WHERE (deleted = false);


--
-- Name: uk_sys_menu_path_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_menu_path_active ON public.sys_menu USING btree (tenant_id, path) WHERE ((deleted = false) AND (menu_type = 'MENU') AND (path IS NOT NULL));




--
-- Name: uk_sys_tenant_code_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_tenant_code_active ON public.sys_tenant USING btree (tenant_code) WHERE (deleted = false);


--
-- Name: uk_sys_user_post_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_user_post_active ON public.sys_user_post USING btree (tenant_id, user_id, post_id) WHERE (deleted = false);


--
-- Name: uk_sys_user_role_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_user_role_active ON public.sys_user_role USING btree (tenant_id, user_id, role_id) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_sys_register_personnel_active ON public.sys_register USING btree (tenant_id, personnel_id) WHERE ((deleted = false) AND ((registration_state)::text <> 'REJECTED'::text));
CREATE UNIQUE INDEX uk_sys_register_user_active ON public.sys_register USING btree (tenant_id, user_id) WHERE ((deleted = false) AND (user_id IS NOT NULL));
CREATE UNIQUE INDEX uk_sys_outbox_registration_event ON public.sys_outbox USING btree (tenant_id, aggregate_id, event_type) WHERE (deleted = false);
CREATE INDEX idx_sys_outbox_ready ON public.sys_outbox USING btree (delivery_state, available_at, create_time) WHERE (deleted = false);


--
-- Name: uk_sys_user_username_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_sys_user_username_active ON public.sys_user USING btree (tenant_id, username) WHERE (deleted = false);


--
-- Name: sys_user_role fkb40xxfch70f5qnyfw8yme1n1s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT fkb40xxfch70f5qnyfw8yme1n1s FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- Name: sys_dept fkd90kntce4nxe9g8qdkb0942j4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dept
    ADD CONSTRAINT fkd90kntce4nxe9g8qdkb0942j4 FOREIGN KEY (region_code) REFERENCES public.sys_region(region_code);


--
-- Name: sys_post fklqy1fcyvh27l0pu89pg2ocpkr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_post
    ADD CONSTRAINT fklqy1fcyvh27l0pu89pg2ocpkr FOREIGN KEY (parent_id) REFERENCES public.sys_post(id);


--
-- Name: sys_user_post fkpjx0gi8xwm66cp1w1jvi4hc57; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_post
    ADD CONSTRAINT fkpjx0gi8xwm66cp1w1jvi4hc57 FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- PostgreSQL database dump complete
--
CREATE UNIQUE INDEX uk_sys_setting_tenant_owner_active ON public.sys_setting USING btree (tenant_id, owner_id) WHERE (deleted = false);

CREATE TABLE public.file_space (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id varchar(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    space_code varchar(128) NOT NULL,
    space_name varchar(256) NOT NULL,
    space_type varchar(20) NOT NULL,
    owner_user_id varchar(64),
    owner_dept_id varchar(64),
    quota_bytes bigint NOT NULL,
    used_bytes bigint NOT NULL DEFAULT 0,
    reserved_bytes bigint NOT NULL DEFAULT 0,
    CONSTRAINT file_space_pkey PRIMARY KEY (id),
    CONSTRAINT ck_file_space_type CHECK (space_type IN ('PERSONAL', 'DEPARTMENT', 'SYSTEM')),
    CONSTRAINT ck_file_space_owner CHECK (
        (space_type = 'PERSONAL' AND owner_user_id IS NOT NULL AND owner_dept_id IS NULL)
        OR (space_type = 'DEPARTMENT' AND owner_user_id IS NULL AND owner_dept_id IS NOT NULL)
        OR (space_type = 'SYSTEM' AND owner_user_id IS NULL AND owner_dept_id IS NULL)
    ),
    CONSTRAINT ck_file_space_quota CHECK (
        quota_bytes > 0 AND used_bytes >= 0 AND reserved_bytes >= 0
        AND used_bytes + reserved_bytes <= quota_bytes
    )
);

COMMENT ON TABLE public.file_space IS '多租户个人、部门和内部业务文件空间';
COMMENT ON COLUMN public.file_space.reserved_bytes IS '正在上传且尚未结算的预留容量字节数';
CREATE UNIQUE INDEX uk_file_space_code_active
    ON public.file_space (tenant_id, space_code)
    WHERE deleted = false;
CREATE UNIQUE INDEX uk_file_space_personal_active
    ON public.file_space (tenant_id, owner_user_id)
    WHERE space_type = 'PERSONAL' AND deleted = false;
CREATE UNIQUE INDEX uk_file_space_department_active
    ON public.file_space (tenant_id, owner_dept_id)
    WHERE space_type = 'DEPARTMENT' AND deleted = false;

CREATE TABLE public.file_node (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id varchar(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    space_id varchar(64) NOT NULL,
    parent_id varchar(64),
    node_type varchar(16) NOT NULL,
    node_name varchar(256) NOT NULL,
    display_path varchar(2048) NOT NULL,
    current_version_id varchar(64),
    active_upload_id varchar(64),
    node_state varchar(16) NOT NULL DEFAULT 'ACTIVE',
    trashed_at timestamp(6) with time zone,
    trashed_by varchar(64),
    business_type varchar(64),
    business_id varchar(128),
    CONSTRAINT file_node_pkey PRIMARY KEY (id),
    CONSTRAINT fk_file_node_space FOREIGN KEY (space_id) REFERENCES public.file_space(id),
    CONSTRAINT fk_file_node_parent FOREIGN KEY (parent_id) REFERENCES public.file_node(id),
    CONSTRAINT ck_file_node_type CHECK (node_type IN ('FILE', 'FOLDER')),
    CONSTRAINT ck_file_node_state CHECK (node_state IN ('UPLOADING', 'ACTIVE', 'TRASHED')),
    CONSTRAINT ck_file_node_version CHECK (
        (node_type = 'FILE') OR (node_type = 'FOLDER' AND current_version_id IS NULL)
    )
);

COMMENT ON TABLE public.file_node IS '多租户文件树节点及回收站状态';
COMMENT ON COLUMN public.file_node.active_upload_id IS '当前占用节点版本分配边界的上传记录标识';
CREATE UNIQUE INDEX uk_file_node_name_active
    ON public.file_node (tenant_id, space_id, parent_id, node_name) NULLS NOT DISTINCT
    WHERE node_state IN ('UPLOADING', 'ACTIVE') AND deleted = false;
CREATE INDEX idx_file_node_tree
    ON public.file_node (tenant_id, space_id, parent_id, node_state, node_name)
    WHERE deleted = false;
CREATE INDEX idx_file_node_path
    ON public.file_node (tenant_id, space_id, display_path text_pattern_ops)
    WHERE deleted = false;
CREATE INDEX idx_file_node_business
    ON public.file_node (tenant_id, business_type, business_id, create_time DESC)
    WHERE business_id IS NOT NULL AND deleted = false;

CREATE TABLE public.file_version (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id varchar(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    node_id varchar(64) NOT NULL,
    version_no integer NOT NULL,
    version_state varchar(16) NOT NULL DEFAULT 'PENDING',
    object_key varchar(1024) NOT NULL,
    original_name varchar(256) NOT NULL,
    content_type varchar(128) NOT NULL,
    size_bytes bigint NOT NULL,
    sha256 varchar(64) NOT NULL,
    CONSTRAINT file_version_pkey PRIMARY KEY (id),
    CONSTRAINT fk_file_version_node FOREIGN KEY (node_id) REFERENCES public.file_node(id),
    CONSTRAINT ck_file_version_no CHECK (version_no > 0),
    CONSTRAINT ck_file_version_state CHECK (version_state IN ('PENDING', 'AVAILABLE', 'CANCELLED')),
    CONSTRAINT ck_file_version_size CHECK (size_bytes > 0)
);

COMMENT ON TABLE public.file_version IS '文件节点的不可变内容版本';
COMMENT ON COLUMN public.file_version.version_state IS '版本内容的预留、可用或取消状态';
CREATE UNIQUE INDEX uk_file_version_no_active
    ON public.file_version (tenant_id, node_id, version_no)
    WHERE deleted = false;
CREATE UNIQUE INDEX uk_file_version_object_key
    ON public.file_version (object_key);
CREATE INDEX idx_file_version_node_time
    ON public.file_version (tenant_id, node_id, create_time DESC)
    WHERE deleted = false;

ALTER TABLE public.file_node
    ADD CONSTRAINT fk_file_node_current_version
    FOREIGN KEY (current_version_id) REFERENCES public.file_version(id);

CREATE TABLE public.file_upload_record (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id varchar(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    space_id varchar(64) NOT NULL,
    node_id varchar(64) NOT NULL,
    version_id varchar(64) NOT NULL,
    object_key varchar(1024) NOT NULL,
    operator_user_id varchar(64) NOT NULL,
    business_type varchar(64),
    business_id varchar(128),
    reserved_bytes bigint NOT NULL,
    new_node boolean NOT NULL,
    upload_state varchar(24) NOT NULL DEFAULT 'PREPARED',
    execution_token varchar(64) NOT NULL,
    lease_expires_at timestamp(6) with time zone NOT NULL,
    next_attempt_at timestamp(6) with time zone NOT NULL,
    attempt_count integer NOT NULL DEFAULT 1,
    last_error varchar(2000),
    CONSTRAINT file_upload_record_pkey PRIMARY KEY (id),
    CONSTRAINT fk_file_upload_space FOREIGN KEY (space_id) REFERENCES public.file_space(id),
    CONSTRAINT fk_file_upload_node FOREIGN KEY (node_id) REFERENCES public.file_node(id),
    CONSTRAINT fk_file_upload_version FOREIGN KEY (version_id) REFERENCES public.file_version(id),
    CONSTRAINT ck_file_upload_state CHECK (
        upload_state IN ('PREPARED', 'OBJECT_STORED', 'COMPLETED', 'CLEANUP_PENDING', 'CANCELLED')
    ),
    CONSTRAINT ck_file_upload_reserved_bytes CHECK (reserved_bytes > 0),
    CONSTRAINT ck_file_upload_attempt_count CHECK (attempt_count > 0),
    CONSTRAINT ck_file_upload_execution_token CHECK (btrim(execution_token) <> '')
);

COMMENT ON TABLE public.file_upload_record IS '连接数据库元数据与对象存储操作的可恢复上传记录';
COMMENT ON COLUMN public.file_upload_record.id IS '上传记录主键';
COMMENT ON COLUMN public.file_upload_record.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.file_upload_record.create_time IS '创建时间';
COMMENT ON COLUMN public.file_upload_record.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.file_upload_record.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.file_upload_record.update_time IS '最后修改时间';
COMMENT ON COLUMN public.file_upload_record.version IS '乐观锁版本号';
COMMENT ON COLUMN public.file_upload_record.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.file_upload_record.enabled IS '是否启用';
COMMENT ON COLUMN public.file_upload_record.remark IS '备注';
COMMENT ON COLUMN public.file_upload_record.status IS '通用数据状态';
COMMENT ON COLUMN public.file_upload_record.space_id IS '所属文件空间标识';
COMMENT ON COLUMN public.file_upload_record.node_id IS '本次上传目标文件节点标识';
COMMENT ON COLUMN public.file_upload_record.version_id IS '本次上传预留的文件版本标识';
COMMENT ON COLUMN public.file_upload_record.object_key IS '本次上传预留的不可变对象键';
COMMENT ON COLUMN public.file_upload_record.operator_user_id IS '发起上传的用户标识';
COMMENT ON COLUMN public.file_upload_record.business_type IS '来源业务类型';
COMMENT ON COLUMN public.file_upload_record.business_id IS '来源业务标识';
COMMENT ON COLUMN public.file_upload_record.reserved_bytes IS '本次上传占用的预留容量字节数';
COMMENT ON COLUMN public.file_upload_record.new_node IS '本次上传是否创建新文件节点';
COMMENT ON COLUMN public.file_upload_record.upload_state IS '上传 saga 当前状态';
COMMENT ON COLUMN public.file_upload_record.execution_token IS '当前执行者的并发控制令牌';
COMMENT ON COLUMN public.file_upload_record.lease_expires_at IS '当前执行者租约到期时间';
COMMENT ON COLUMN public.file_upload_record.next_attempt_at IS '失败后下次可恢复时间';
COMMENT ON COLUMN public.file_upload_record.attempt_count IS '上传与恢复执行次数';
COMMENT ON COLUMN public.file_upload_record.last_error IS '最近一次失败的受限长度摘要';

CREATE UNIQUE INDEX uk_file_upload_version
    ON public.file_upload_record (tenant_id, version_id);
CREATE UNIQUE INDEX uk_file_upload_node_open
    ON public.file_upload_record (tenant_id, node_id)
    WHERE upload_state IN ('PREPARED', 'OBJECT_STORED', 'CLEANUP_PENDING') AND deleted = false;
CREATE INDEX idx_file_upload_reconcile
    ON public.file_upload_record (next_attempt_at, lease_expires_at, upload_state)
    WHERE upload_state IN ('PREPARED', 'OBJECT_STORED', 'CLEANUP_PENDING') AND deleted = false;

CREATE TABLE public.file_grant (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id varchar(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    space_id varchar(64) NOT NULL,
    node_id varchar(64),
    principal_type varchar(20) NOT NULL,
    principal_id varchar(64) NOT NULL,
    grant_role varchar(20) NOT NULL,
    inherited boolean NOT NULL DEFAULT true,
    expires_at timestamp(6) with time zone,
    CONSTRAINT file_grant_pkey PRIMARY KEY (id),
    CONSTRAINT fk_file_grant_space FOREIGN KEY (space_id) REFERENCES public.file_space(id),
    CONSTRAINT fk_file_grant_node FOREIGN KEY (node_id) REFERENCES public.file_node(id),
    CONSTRAINT ck_file_grant_principal CHECK (principal_type IN ('USER', 'DEPARTMENT')),
    CONSTRAINT ck_file_grant_role CHECK (grant_role IN ('VIEWER', 'EDITOR', 'MANAGER'))
);

COMMENT ON TABLE public.file_grant IS '面向用户或部门的空间级、节点级共享授权';
CREATE UNIQUE INDEX uk_file_grant_principal_active
    ON public.file_grant (tenant_id, space_id, node_id, principal_type, principal_id) NULLS NOT DISTINCT
    WHERE deleted = false;
CREATE INDEX idx_file_grant_lookup
    ON public.file_grant (tenant_id, space_id, principal_type, principal_id, expires_at)
    WHERE deleted = false;

CREATE TABLE public.file_edit_lock (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id varchar(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    node_id varchar(64) NOT NULL,
    owner_user_id varchar(64) NOT NULL,
    token_hash varchar(64) NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT file_edit_lock_pkey PRIMARY KEY (id),
    CONSTRAINT fk_file_edit_lock_node FOREIGN KEY (node_id) REFERENCES public.file_node(id)
);

COMMENT ON TABLE public.file_edit_lock IS '文件协同编辑的短时互斥锁';
CREATE UNIQUE INDEX uk_file_edit_lock_node_active
    ON public.file_edit_lock (tenant_id, node_id)
    WHERE deleted = false;
CREATE INDEX idx_file_edit_lock_expiry
    ON public.file_edit_lock (expires_at)
    WHERE deleted = false;

CREATE TABLE public.file_operation_log (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id varchar(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    operator_user_id varchar(64) NOT NULL,
    operation varchar(32) NOT NULL,
    space_id varchar(64) NOT NULL,
    node_id varchar(64),
    detail varchar(1000),
    CONSTRAINT file_operation_log_pkey PRIMARY KEY (id)
);

COMMENT ON TABLE public.file_operation_log IS '文件访问、编辑、共享和回收操作审计';
CREATE INDEX idx_file_operation_log_target
    ON public.file_operation_log (tenant_id, space_id, node_id, create_time DESC);
CREATE INDEX idx_file_operation_log_operator
    ON public.file_operation_log (tenant_id, operator_user_id, create_time DESC);
CREATE TABLE public.sse_connection_record (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    enabled boolean,
    remark varchar(500),
    sort_order integer,
    tenant_id varchar(64) NOT NULL,
    client_ip varchar(64),
    connect_time timestamp(6) without time zone NOT NULL,
    connection_id varchar(64) NOT NULL,
    connection_status varchar(20),
    disconnect_reason varchar(50),
    disconnect_time timestamp(6) without time zone,
    duration_seconds bigint,
    server_instance varchar(128),
    user_id varchar(64) NOT NULL,
    CONSTRAINT sse_connection_record_pkey PRIMARY KEY (id)
);

COMMENT ON TABLE public.sse_connection_record IS 'SSE 连接记录表';
CREATE INDEX idx_conn_user_id ON public.sse_connection_record (user_id);
CREATE INDEX idx_conn_status ON public.sse_connection_record (connection_status);
CREATE INDEX idx_conn_connect_time ON public.sse_connection_record (connect_time);
CREATE INDEX idx_conn_tenant_time ON public.sse_connection_record (tenant_id, connect_time DESC);

CREATE TABLE public.sse_push_log (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    enabled boolean,
    remark varchar(500),
    sort_order integer,
    tenant_id varchar(64) NOT NULL,
    fail_reason varchar(500),
    message_id varchar(128),
    message_type varchar(50),
    push_content varchar(5000),
    push_status varchar(20),
    push_time timestamp(6) without time zone,
    push_title varchar(256),
    retry_count integer,
    target_type varchar(20),
    user_id varchar(64),
    user_ids varchar(2000),
    CONSTRAINT sse_push_log_pkey PRIMARY KEY (id)
);

COMMENT ON TABLE public.sse_push_log IS 'SSE 推送日志表';
CREATE INDEX idx_push_log_message_id ON public.sse_push_log (message_id);
CREATE INDEX idx_push_log_user_id ON public.sse_push_log (user_id);
CREATE INDEX idx_push_log_push_time ON public.sse_push_log (push_time);
CREATE INDEX idx_push_log_tenant_time ON public.sse_push_log (tenant_id, push_time DESC);

CREATE TABLE public.sse_op_log (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    enabled boolean,
    remark varchar(500),
    sort_order integer,
    tenant_id varchar(64) NOT NULL,
    client_ip varchar(50),
    location varchar(100),
    status varchar(20) NOT NULL,
    log_time timestamp(6) without time zone,
    message varchar(500),
    trace_id varchar(64),
    user_id varchar(64),
    username varchar(64),
    connection_id varchar(64),
    content text,
    cost_ms bigint,
    description varchar(500),
    fail_reason varchar(500),
    message_type varchar(50),
    operation_id varchar(64),
    operation_type varchar(20) NOT NULL,
    target_type varchar(20),
    CONSTRAINT ck_sse_op_log_operation_type CHECK (operation_type IN ('CONNECT', 'DISCONNECT', 'PUSH')),
    CONSTRAINT sse_op_log_pkey PRIMARY KEY (id)
);

COMMENT ON TABLE public.sse_op_log IS 'SSE 操作日志表';
CREATE INDEX idx_sse_op_log_user ON public.sse_op_log (user_id);
CREATE INDEX idx_sse_op_log_time ON public.sse_op_log (log_time);
CREATE INDEX idx_sse_op_log_type ON public.sse_op_log (operation_type);
CREATE INDEX idx_sse_op_log_conn ON public.sse_op_log (connection_id);
CREATE INDEX idx_sse_op_log_tenant_time ON public.sse_op_log (tenant_id, log_time DESC);
CREATE TABLE public.scheduler_task (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer,
    tenant_id varchar(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    task_code varchar(128) NOT NULL,
    task_name varchar(256) NOT NULL,
    application_code varchar(64) NOT NULL,
    application_name varchar(128) NOT NULL,
    processor_info varchar(256) NOT NULL,
    time_expression_type varchar(32) NOT NULL,
    time_expression varchar(256) NOT NULL,
    job_parameters text,
    max_instance_num integer NOT NULL DEFAULT 1,
    concurrency integer NOT NULL DEFAULT 1,
    instance_time_limit bigint NOT NULL DEFAULT 0,
    instance_retry_num integer NOT NULL DEFAULT 0,
    task_retry_num integer NOT NULL DEFAULT 0,
    powerjob_job_id bigint,
    sync_state varchar(20) NOT NULL DEFAULT 'PENDING',
    last_sync_time timestamp(6) without time zone,
    last_sync_message varchar(1000),
    CONSTRAINT scheduler_task_pkey PRIMARY KEY (id),
    CONSTRAINT ck_scheduler_expression_type CHECK (
        time_expression_type IN ('CRON', 'FIXED_RATE', 'FIXED_DELAY')
    ),
    CONSTRAINT ck_scheduler_sync_state CHECK (sync_state IN ('PENDING', 'SYNCED', 'FAILED')),
    CONSTRAINT ck_scheduler_limits CHECK (
        max_instance_num >= 0
        AND concurrency > 0
        AND instance_time_limit >= 0
        AND instance_retry_num >= 0
        AND task_retry_num >= 0
    )
);

COMMENT ON TABLE public.scheduler_task IS '租户隔离的通用调度控制面任务定义';
COMMENT ON COLUMN public.scheduler_task.application_code IS 'Nacos 中配置的产品应用逻辑编码';
COMMENT ON COLUMN public.scheduler_task.application_name IS 'PowerJob 应用名称快照';
COMMENT ON COLUMN public.scheduler_task.processor_info IS '业务服务内 PowerJob 处理器 Bean 名称';
COMMENT ON COLUMN public.scheduler_task.sync_state IS '本地期望定义与 PowerJob 的同步状态';

CREATE UNIQUE INDEX uk_scheduler_task_code_active
    ON public.scheduler_task (tenant_id, task_code)
    WHERE deleted = false;
CREATE UNIQUE INDEX uk_scheduler_powerjob_job_active
    ON public.scheduler_task (powerjob_job_id)
    WHERE powerjob_job_id IS NOT NULL AND deleted = false;
CREATE INDEX idx_scheduler_task_tenant_state
    ON public.scheduler_task (tenant_id, enabled, sync_state, create_time DESC)
    WHERE deleted = false;
-- 通用 Agent 是 ai-platform 的平台能力，定义、运行记录与审计数据统一进入 public schema。
CREATE TABLE public.ai_agent_config (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer NOT NULL DEFAULT 0,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    tenant_id varchar(64) NOT NULL,
    agent_code varchar(128) NOT NULL,
    agent_name varchar(256) NOT NULL,
    description varchar(1000),
    provider_type varchar(32) NOT NULL,
    invocation_mode varchar(32) NOT NULL,
    endpoint_url varchar(1000) NOT NULL,
    model_name varchar(256),
    credential varchar(2000),
    system_prompt text,
    temperature double precision,
    runtime_config text,
    owner_user_id varchar(64),
    publish_state varchar(32) NOT NULL DEFAULT 'DRAFT',
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_ai_agent_config PRIMARY KEY (id),
    CONSTRAINT uk_ai_agent_config_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_ai_agent_config_tenant CHECK (btrim(tenant_id) <> ''),
    CONSTRAINT ck_ai_agent_config_code CHECK (btrim(agent_code) <> ''),
    CONSTRAINT ck_ai_agent_config_provider CHECK (provider_type IN ('OPENAI_COMPATIBLE', 'DIFY')),
    CONSTRAINT ck_ai_agent_config_mode CHECK (invocation_mode IN ('CHAT', 'WORKFLOW')),
    CONSTRAINT ck_ai_agent_config_publish CHECK (publish_state IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_ai_agent_config_temperature CHECK (temperature IS NULL OR temperature BETWEEN 0.0 AND 2.0),
    CONSTRAINT ck_ai_agent_config_revision CHECK (revision > 0)
);

COMMENT ON TABLE public.ai_agent_config IS '租户隔离的通用智能体定义与发布配置';
COMMENT ON COLUMN public.ai_agent_config.id IS '主键';
COMMENT ON COLUMN public.ai_agent_config.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.ai_agent_config.create_time IS '创建时间';
COMMENT ON COLUMN public.ai_agent_config.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.ai_agent_config.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.ai_agent_config.update_time IS '最后修改时间';
COMMENT ON COLUMN public.ai_agent_config.version IS '乐观锁版本号';
COMMENT ON COLUMN public.ai_agent_config.enabled IS '是否启用';
COMMENT ON COLUMN public.ai_agent_config.remark IS '备注';
COMMENT ON COLUMN public.ai_agent_config.status IS '通用数据状态';
COMMENT ON COLUMN public.ai_agent_config.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.ai_agent_config.agent_code IS '租户内稳定且唯一的智能体编码';
COMMENT ON COLUMN public.ai_agent_config.agent_name IS '智能体名称';
COMMENT ON COLUMN public.ai_agent_config.description IS '智能体说明';
COMMENT ON COLUMN public.ai_agent_config.provider_type IS '提供方协议类型';
COMMENT ON COLUMN public.ai_agent_config.invocation_mode IS '调用模式';
COMMENT ON COLUMN public.ai_agent_config.endpoint_url IS '提供方接口基础地址';
COMMENT ON COLUMN public.ai_agent_config.model_name IS '模型或提供方应用名称';
COMMENT ON COLUMN public.ai_agent_config.credential IS '按平台密钥版本加密的提供方凭据';
COMMENT ON COLUMN public.ai_agent_config.system_prompt IS '系统提示词';
COMMENT ON COLUMN public.ai_agent_config.temperature IS '采样温度';
COMMENT ON COLUMN public.ai_agent_config.runtime_config IS '产品无关的提供方扩展配置 JSON';
COMMENT ON COLUMN public.ai_agent_config.owner_user_id IS '配置负责人用户标识';
COMMENT ON COLUMN public.ai_agent_config.publish_state IS '发布状态';
COMMENT ON COLUMN public.ai_agent_config.revision IS '配置修订号';

CREATE UNIQUE INDEX uk_ai_agent_config_tenant_code_active
    ON public.ai_agent_config (tenant_id, agent_code)
    WHERE deleted = false;
CREATE INDEX idx_ai_agent_config_tenant_publish
    ON public.ai_agent_config (tenant_id, publish_state, enabled, create_time DESC)
    WHERE deleted = false;

CREATE TABLE public.ai_agent_session_record (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer NOT NULL DEFAULT 0,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    tenant_id varchar(64) NOT NULL,
    session_code varchar(128) NOT NULL,
    agent_id varchar(64) NOT NULL,
    agent_code varchar(128) NOT NULL,
    user_id varchar(64) NOT NULL,
    title varchar(256),
    context_namespace varchar(128),
    context_reference varchar(256),
    context_json text,
    provider_conversation_id varchar(256),
    active_invocation_id varchar(128),
    message_count integer NOT NULL DEFAULT 0,
    session_state varchar(32) NOT NULL DEFAULT 'ACTIVE',
    started_at timestamp(6) without time zone NOT NULL,
    ended_at timestamp(6) without time zone,
    CONSTRAINT pk_ai_agent_session_record PRIMARY KEY (id),
    CONSTRAINT uk_ai_agent_session_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_ai_agent_session_definition FOREIGN KEY (tenant_id, agent_id)
        REFERENCES public.ai_agent_config (tenant_id, id),
    CONSTRAINT ck_ai_agent_session_tenant CHECK (btrim(tenant_id) <> ''),
    CONSTRAINT ck_ai_agent_session_code CHECK (btrim(session_code) <> ''),
    CONSTRAINT ck_ai_agent_session_state CHECK (session_state IN ('ACTIVE', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_ai_agent_session_message_count CHECK (message_count >= 0),
    CONSTRAINT ck_ai_agent_session_time CHECK (ended_at IS NULL OR ended_at >= started_at)
);

COMMENT ON TABLE public.ai_agent_session_record IS '通用智能体会话记录';
COMMENT ON COLUMN public.ai_agent_session_record.id IS '主键';
COMMENT ON COLUMN public.ai_agent_session_record.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.ai_agent_session_record.create_time IS '创建时间';
COMMENT ON COLUMN public.ai_agent_session_record.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.ai_agent_session_record.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.ai_agent_session_record.update_time IS '最后修改时间';
COMMENT ON COLUMN public.ai_agent_session_record.version IS '乐观锁版本号';
COMMENT ON COLUMN public.ai_agent_session_record.enabled IS '是否启用';
COMMENT ON COLUMN public.ai_agent_session_record.remark IS '备注';
COMMENT ON COLUMN public.ai_agent_session_record.status IS '通用数据状态';
COMMENT ON COLUMN public.ai_agent_session_record.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.ai_agent_session_record.session_code IS '租户内会话编码';
COMMENT ON COLUMN public.ai_agent_session_record.agent_id IS '智能体定义主键';
COMMENT ON COLUMN public.ai_agent_session_record.agent_code IS '调用时的智能体编码快照';
COMMENT ON COLUMN public.ai_agent_session_record.user_id IS '会话所属用户标识';
COMMENT ON COLUMN public.ai_agent_session_record.title IS '会话标题';
COMMENT ON COLUMN public.ai_agent_session_record.context_namespace IS '产品定义的通用上下文命名空间';
COMMENT ON COLUMN public.ai_agent_session_record.context_reference IS '产品定义的通用上下文引用';
COMMENT ON COLUMN public.ai_agent_session_record.context_json IS '产品无关的上下文扩展 JSON';
COMMENT ON COLUMN public.ai_agent_session_record.provider_conversation_id IS '提供方连续对话标识';
COMMENT ON COLUMN public.ai_agent_session_record.active_invocation_id IS '当前占用会话顺序边界的调用幂等标识';
COMMENT ON COLUMN public.ai_agent_session_record.message_count IS '已持久化或已被活动调用预留的最大消息序号';
COMMENT ON COLUMN public.ai_agent_session_record.session_state IS '会话状态';
COMMENT ON COLUMN public.ai_agent_session_record.started_at IS '会话开始时间';
COMMENT ON COLUMN public.ai_agent_session_record.ended_at IS '会话结束时间';

CREATE UNIQUE INDEX uk_ai_agent_session_tenant_code_active
    ON public.ai_agent_session_record (tenant_id, session_code)
    WHERE deleted = false;
CREATE INDEX idx_ai_agent_session_tenant_agent
    ON public.ai_agent_session_record (tenant_id, agent_id, create_time DESC)
    WHERE deleted = false;
CREATE INDEX idx_ai_agent_session_tenant_user
    ON public.ai_agent_session_record (tenant_id, user_id, create_time DESC)
    WHERE deleted = false;

CREATE TABLE public.ai_agent_message_record (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer NOT NULL DEFAULT 0,
    tenant_id varchar(64) NOT NULL,
    invocation_id varchar(128) NOT NULL,
    session_id varchar(64) NOT NULL,
    sequence_no integer NOT NULL,
    role varchar(32) NOT NULL,
    content text NOT NULL,
    metadata_json text,
    CONSTRAINT pk_ai_agent_message_record PRIMARY KEY (id),
    CONSTRAINT uk_ai_agent_message_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_ai_agent_message_session FOREIGN KEY (tenant_id, session_id)
        REFERENCES public.ai_agent_session_record (tenant_id, id),
    CONSTRAINT ck_ai_agent_message_tenant CHECK (btrim(tenant_id) <> ''),
    CONSTRAINT ck_ai_agent_message_invocation CHECK (btrim(invocation_id) <> ''),
    CONSTRAINT ck_ai_agent_message_sequence CHECK (sequence_no > 0),
    CONSTRAINT ck_ai_agent_message_role CHECK (role IN ('user', 'assistant', 'system', 'tool'))
);

COMMENT ON TABLE public.ai_agent_message_record IS '通用智能体会话中的有序消息';
COMMENT ON COLUMN public.ai_agent_message_record.id IS '主键';
COMMENT ON COLUMN public.ai_agent_message_record.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.ai_agent_message_record.create_time IS '创建时间';
COMMENT ON COLUMN public.ai_agent_message_record.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.ai_agent_message_record.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.ai_agent_message_record.update_time IS '最后修改时间';
COMMENT ON COLUMN public.ai_agent_message_record.version IS '乐观锁版本号';
COMMENT ON COLUMN public.ai_agent_message_record.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.ai_agent_message_record.invocation_id IS '产生此消息的调用幂等标识';
COMMENT ON COLUMN public.ai_agent_message_record.session_id IS '所属会话主键';
COMMENT ON COLUMN public.ai_agent_message_record.sequence_no IS '会话内消息序号';
COMMENT ON COLUMN public.ai_agent_message_record.role IS '消息角色';
COMMENT ON COLUMN public.ai_agent_message_record.content IS '消息正文';
COMMENT ON COLUMN public.ai_agent_message_record.metadata_json IS '产品无关的消息元数据 JSON';

CREATE UNIQUE INDEX uk_ai_agent_message_session_sequence_active
    ON public.ai_agent_message_record (tenant_id, session_id, sequence_no)
    WHERE deleted = false;
CREATE UNIQUE INDEX uk_ai_agent_message_invocation_role_active
    ON public.ai_agent_message_record (tenant_id, session_id, invocation_id, role)
    WHERE deleted = false;
CREATE INDEX idx_ai_agent_message_session
    ON public.ai_agent_message_record (tenant_id, session_id, create_time)
    WHERE deleted = false;

CREATE TABLE public.ai_agent_call_record (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer NOT NULL DEFAULT 0,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    tenant_id varchar(64) NOT NULL,
    invocation_code varchar(128) NOT NULL,
    request_fingerprint varchar(64) NOT NULL,
    execution_token varchar(64) NOT NULL,
    lease_expires_at timestamp(6) without time zone NOT NULL,
    attempt_count integer NOT NULL DEFAULT 1,
    session_id varchar(64) NOT NULL,
    user_message_id varchar(64) NOT NULL,
    assistant_message_id varchar(64),
    reserved_user_sequence_no integer NOT NULL,
    reserved_assistant_sequence_no integer NOT NULL,
    agent_id varchar(64) NOT NULL,
    agent_code varchar(128) NOT NULL,
    provider_type varchar(32) NOT NULL,
    model_name varchar(256),
    operation varchar(64) NOT NULL,
    invocation_state varchar(32) NOT NULL DEFAULT 'RUNNING',
    input_tokens integer,
    output_tokens integer,
    latency_ms bigint,
    request_summary varchar(2000),
    response_summary varchar(4000),
    provider_response_text text,
    provider_conversation_id varchar(256),
    error_code varchar(128),
    error_message varchar(2000),
    CONSTRAINT pk_ai_agent_call_record PRIMARY KEY (id),
    CONSTRAINT fk_ai_agent_call_definition FOREIGN KEY (tenant_id, agent_id)
        REFERENCES public.ai_agent_config (tenant_id, id),
    CONSTRAINT fk_ai_agent_call_session FOREIGN KEY (tenant_id, session_id)
        REFERENCES public.ai_agent_session_record (tenant_id, id),
    CONSTRAINT fk_ai_agent_call_user_message FOREIGN KEY (tenant_id, user_message_id)
        REFERENCES public.ai_agent_message_record (tenant_id, id),
    CONSTRAINT fk_ai_agent_call_assistant_message FOREIGN KEY (tenant_id, assistant_message_id)
        REFERENCES public.ai_agent_message_record (tenant_id, id),
    CONSTRAINT ck_ai_agent_call_tenant CHECK (btrim(tenant_id) <> ''),
    CONSTRAINT ck_ai_agent_call_invocation CHECK (btrim(invocation_code) <> ''),
    CONSTRAINT ck_ai_agent_call_fingerprint CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_ai_agent_call_execution_token CHECK (btrim(execution_token) <> ''),
    CONSTRAINT ck_ai_agent_call_provider CHECK (provider_type IN ('OPENAI_COMPATIBLE', 'DIFY')),
    CONSTRAINT ck_ai_agent_call_state CHECK (invocation_state IN ('RUNNING', 'PROVIDER_SUCCEEDED', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_ai_agent_call_attempt_count CHECK (attempt_count > 0),
    CONSTRAINT ck_ai_agent_call_reserved_sequence CHECK (
        reserved_user_sequence_no > 0
        AND reserved_assistant_sequence_no = reserved_user_sequence_no + 1
    ),
    CONSTRAINT ck_ai_agent_call_input_tokens CHECK (input_tokens IS NULL OR input_tokens >= 0),
    CONSTRAINT ck_ai_agent_call_output_tokens CHECK (output_tokens IS NULL OR output_tokens >= 0),
    CONSTRAINT ck_ai_agent_call_latency CHECK (latency_ms IS NULL OR latency_ms >= 0)
);

COMMENT ON TABLE public.ai_agent_call_record IS '不记录密钥的智能体调用状态与审计记录';
COMMENT ON COLUMN public.ai_agent_call_record.id IS '主键';
COMMENT ON COLUMN public.ai_agent_call_record.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.ai_agent_call_record.create_time IS '创建时间';
COMMENT ON COLUMN public.ai_agent_call_record.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.ai_agent_call_record.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.ai_agent_call_record.update_time IS '最后修改时间';
COMMENT ON COLUMN public.ai_agent_call_record.version IS '乐观锁版本号';
COMMENT ON COLUMN public.ai_agent_call_record.enabled IS '是否启用';
COMMENT ON COLUMN public.ai_agent_call_record.remark IS '备注';
COMMENT ON COLUMN public.ai_agent_call_record.status IS '通用数据状态';
COMMENT ON COLUMN public.ai_agent_call_record.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.ai_agent_call_record.invocation_code IS '租户内调用幂等标识';
COMMENT ON COLUMN public.ai_agent_call_record.request_fingerprint IS '用于拒绝幂等标识重用于不同请求的 SHA-256 指纹';
COMMENT ON COLUMN public.ai_agent_call_record.execution_token IS '当前执行尝试的 CAS 令牌';
COMMENT ON COLUMN public.ai_agent_call_record.lease_expires_at IS '当前执行尝试的租约截止时间';
COMMENT ON COLUMN public.ai_agent_call_record.attempt_count IS '调用执行尝试次数';
COMMENT ON COLUMN public.ai_agent_call_record.session_id IS '所属会话主键';
COMMENT ON COLUMN public.ai_agent_call_record.user_message_id IS '预留阶段已持久化的用户消息主键';
COMMENT ON COLUMN public.ai_agent_call_record.assistant_message_id IS '完成阶段持久化的助手消息主键';
COMMENT ON COLUMN public.ai_agent_call_record.reserved_user_sequence_no IS '为用户消息预留的会话序号';
COMMENT ON COLUMN public.ai_agent_call_record.reserved_assistant_sequence_no IS '为助手消息预留的会话序号';
COMMENT ON COLUMN public.ai_agent_call_record.agent_id IS '智能体定义主键';
COMMENT ON COLUMN public.ai_agent_call_record.agent_code IS '调用时的智能体编码快照';
COMMENT ON COLUMN public.ai_agent_call_record.provider_type IS '调用时的提供方协议快照';
COMMENT ON COLUMN public.ai_agent_call_record.model_name IS '调用时的模型或应用名称快照';
COMMENT ON COLUMN public.ai_agent_call_record.operation IS '调用操作';
COMMENT ON COLUMN public.ai_agent_call_record.invocation_state IS '调用状态';
COMMENT ON COLUMN public.ai_agent_call_record.input_tokens IS '输入 Token 数';
COMMENT ON COLUMN public.ai_agent_call_record.output_tokens IS '输出 Token 数';
COMMENT ON COLUMN public.ai_agent_call_record.latency_ms IS '调用耗时毫秒数';
COMMENT ON COLUMN public.ai_agent_call_record.request_summary IS '不含敏感正文的请求摘要';
COMMENT ON COLUMN public.ai_agent_call_record.response_summary IS '受限长度的响应摘要';
COMMENT ON COLUMN public.ai_agent_call_record.provider_response_text IS '提供方成功响应正文，用于完成阶段故障恢复';
COMMENT ON COLUMN public.ai_agent_call_record.provider_conversation_id IS '提供方连续对话标识快照';
COMMENT ON COLUMN public.ai_agent_call_record.error_code IS '错误编码';
COMMENT ON COLUMN public.ai_agent_call_record.error_message IS '受限长度的错误信息';

CREATE UNIQUE INDEX uk_ai_agent_call_tenant_code_active
    ON public.ai_agent_call_record (tenant_id, invocation_code)
    WHERE deleted = false;
CREATE INDEX idx_ai_agent_call_tenant_agent
    ON public.ai_agent_call_record (tenant_id, agent_id, create_time DESC)
    WHERE deleted = false;
CREATE INDEX idx_ai_agent_call_session
    ON public.ai_agent_call_record (tenant_id, session_id, create_time)
    WHERE deleted = false;

CREATE TABLE public.ai_agent_voice_record (
    id varchar(64) NOT NULL,
    create_by varchar(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL DEFAULT false,
    update_by varchar(64),
    update_time timestamp(6) without time zone,
    version integer NOT NULL DEFAULT 0,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(500),
    status varchar(20),
    tenant_id varchar(64) NOT NULL,
    session_id varchar(64),
    user_id varchar(64) NOT NULL,
    file_name varchar(256) NOT NULL,
    content_type varchar(128) NOT NULL,
    content_length bigint NOT NULL,
    language varchar(32),
    duration_seconds integer,
    transcript text,
    recognition_status varchar(32) NOT NULL,
    error_message varchar(2000),
    CONSTRAINT pk_ai_agent_voice_record PRIMARY KEY (id),
    CONSTRAINT fk_ai_agent_voice_session FOREIGN KEY (tenant_id, session_id)
        REFERENCES public.ai_agent_session_record (tenant_id, id),
    CONSTRAINT ck_ai_agent_voice_tenant CHECK (btrim(tenant_id) <> ''),
    CONSTRAINT ck_ai_agent_voice_length CHECK (content_length >= 0),
    CONSTRAINT ck_ai_agent_voice_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    CONSTRAINT ck_ai_agent_voice_status CHECK (recognition_status IN ('RUNNING', 'SUCCEEDED', 'NEED_REVIEW', 'FAILED'))
);

COMMENT ON TABLE public.ai_agent_voice_record IS '语音转写请求与结果审计';
COMMENT ON COLUMN public.ai_agent_voice_record.id IS '主键';
COMMENT ON COLUMN public.ai_agent_voice_record.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.ai_agent_voice_record.create_time IS '创建时间';
COMMENT ON COLUMN public.ai_agent_voice_record.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.ai_agent_voice_record.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.ai_agent_voice_record.update_time IS '最后修改时间';
COMMENT ON COLUMN public.ai_agent_voice_record.version IS '乐观锁版本号';
COMMENT ON COLUMN public.ai_agent_voice_record.enabled IS '是否启用';
COMMENT ON COLUMN public.ai_agent_voice_record.remark IS '备注';
COMMENT ON COLUMN public.ai_agent_voice_record.status IS '通用数据状态';
COMMENT ON COLUMN public.ai_agent_voice_record.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.ai_agent_voice_record.session_id IS '可选的关联会话主键';
COMMENT ON COLUMN public.ai_agent_voice_record.user_id IS '发起转写的用户标识';
COMMENT ON COLUMN public.ai_agent_voice_record.file_name IS '原始文件名';
COMMENT ON COLUMN public.ai_agent_voice_record.content_type IS '媒体类型';
COMMENT ON COLUMN public.ai_agent_voice_record.content_length IS '文件字节数';
COMMENT ON COLUMN public.ai_agent_voice_record.language IS '识别语言提示';
COMMENT ON COLUMN public.ai_agent_voice_record.duration_seconds IS '语音时长秒数';
COMMENT ON COLUMN public.ai_agent_voice_record.transcript IS '转写文本';
COMMENT ON COLUMN public.ai_agent_voice_record.recognition_status IS '转写状态';
COMMENT ON COLUMN public.ai_agent_voice_record.error_message IS '受限长度的错误信息';

CREATE INDEX idx_ai_agent_voice_tenant_status
    ON public.ai_agent_voice_record (tenant_id, recognition_status, create_time DESC)
    WHERE deleted = false;
CREATE TABLE public.ai_knowledge_base (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    scope_type character varying(16) DEFAULT 'DEPT'::character varying NOT NULL,
    knowledge_code character varying(64) NOT NULL,
    knowledge_name character varying(128) NOT NULL,
    description character varying(1000),
    top_k integer DEFAULT 5 NOT NULL,
    max_context_chars integer DEFAULT 12000 NOT NULL,
    CONSTRAINT ck_ai_knowledge_base_context_chars CHECK (((max_context_chars >= 1000) AND (max_context_chars <= 50000))),
    CONSTRAINT ck_ai_knowledge_base_dept_scope CHECK (((((scope_type)::text = 'TENANT'::text) AND (dept_id IS NULL)) OR (((scope_type)::text = 'DEPT'::text) AND (dept_id IS NOT NULL)))),
    CONSTRAINT ck_ai_knowledge_base_scope CHECK (((scope_type)::text = ANY ((ARRAY['TENANT'::character varying, 'DEPT'::character varying])::text[]))),
    CONSTRAINT ck_ai_knowledge_base_top_k CHECK (((top_k >= 1) AND (top_k <= 20)))
);

COMMENT ON TABLE public.ai_knowledge_base IS '智能体知识库表';

COMMENT ON COLUMN public.ai_knowledge_base.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_base.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_base.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_base.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_base.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_base.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_base.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_base.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_base.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_base.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_base.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_base.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_base.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_base.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_base.scope_type IS '知识库作用域：TENANT租户兜底、DEPT部门专属';

COMMENT ON COLUMN public.ai_knowledge_base.knowledge_code IS '租户内唯一知识库编码';

COMMENT ON COLUMN public.ai_knowledge_base.knowledge_name IS '知识库名称';

COMMENT ON COLUMN public.ai_knowledge_base.description IS '知识库说明';

COMMENT ON COLUMN public.ai_knowledge_base.top_k IS '每次检索最多返回分块数';

COMMENT ON COLUMN public.ai_knowledge_base.max_context_chars IS '单次拼装上下文最大字符数';

CREATE TABLE public.ai_knowledge_chunk (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    knowledge_base_id character varying(64) NOT NULL,
    document_id character varying(64) NOT NULL,
    chunk_index integer NOT NULL,
    chunk_title character varying(256),
    content text NOT NULL,
    keywords character varying(1000),
    char_count integer DEFAULT 0 NOT NULL,
    content_hash character varying(64) NOT NULL,
    CONSTRAINT ck_ai_knowledge_chunk_char_count CHECK ((char_count >= 0)),
    CONSTRAINT ck_ai_knowledge_chunk_index CHECK ((chunk_index >= 0))
);

COMMENT ON TABLE public.ai_knowledge_chunk IS '知识库文档分块表';

COMMENT ON COLUMN public.ai_knowledge_chunk.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_chunk.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_chunk.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_chunk.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_chunk.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_chunk.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_chunk.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_chunk.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_chunk.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_chunk.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_chunk.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_chunk.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_chunk.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_chunk.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_chunk.knowledge_base_id IS '所属知识库ID';

COMMENT ON COLUMN public.ai_knowledge_chunk.document_id IS '所属文档ID';

COMMENT ON COLUMN public.ai_knowledge_chunk.chunk_index IS '文档内分块序号，从0开始';

COMMENT ON COLUMN public.ai_knowledge_chunk.chunk_title IS '分块标题';

COMMENT ON COLUMN public.ai_knowledge_chunk.content IS '分块正文';

COMMENT ON COLUMN public.ai_knowledge_chunk.keywords IS '人工补充检索关键词';

COMMENT ON COLUMN public.ai_knowledge_chunk.char_count IS '分块正文字符数';

COMMENT ON COLUMN public.ai_knowledge_chunk.content_hash IS '分块正文SHA-256摘要';

CREATE TABLE public.ai_knowledge_context (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    knowledge_base_id character varying(64) NOT NULL,
    context_name character varying(128) NOT NULL,
    context_type character varying(24) DEFAULT 'RETRIEVAL'::character varying NOT NULL,
    trigger_keywords character varying(1000),
    context_content text NOT NULL,
    priority integer DEFAULT 100 NOT NULL,
    max_chars integer DEFAULT 3000 NOT NULL,
    effective_from timestamp without time zone,
    effective_to timestamp without time zone,
    CONSTRAINT ck_ai_knowledge_context_chars CHECK (((max_chars >= 100) AND (max_chars <= 20000))),
    CONSTRAINT ck_ai_knowledge_context_effective CHECK (((effective_to IS NULL) OR (effective_from IS NULL) OR (effective_to > effective_from))),
    CONSTRAINT ck_ai_knowledge_context_priority CHECK (((priority >= 0) AND (priority <= 1000))),
    CONSTRAINT ck_ai_knowledge_context_type CHECK (((context_type)::text = ANY ((ARRAY['SYSTEM'::character varying, 'DOMAIN'::character varying, 'RETRIEVAL'::character varying])::text[])))
);

COMMENT ON TABLE public.ai_knowledge_context IS '知识库上下文规则表';

COMMENT ON COLUMN public.ai_knowledge_context.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_context.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_context.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_context.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_context.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_context.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_context.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_context.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_context.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_context.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_context.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_context.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_context.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_context.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_context.knowledge_base_id IS '所属知识库ID';

COMMENT ON COLUMN public.ai_knowledge_context.context_name IS '上下文规则名称';

COMMENT ON COLUMN public.ai_knowledge_context.context_type IS '上下文类型：SYSTEM、DOMAIN、RETRIEVAL';

COMMENT ON COLUMN public.ai_knowledge_context.trigger_keywords IS '触发词，多个值用逗号或换行分隔；为空表示始终生效';

COMMENT ON COLUMN public.ai_knowledge_context.context_content IS '注入模型的上下文内容';

COMMENT ON COLUMN public.ai_knowledge_context.priority IS '上下文优先级，数值越小越先拼装';

COMMENT ON COLUMN public.ai_knowledge_context.max_chars IS '该规则最多注入字符数';

COMMENT ON COLUMN public.ai_knowledge_context.effective_from IS '生效开始时间';

COMMENT ON COLUMN public.ai_knowledge_context.effective_to IS '生效结束时间';

CREATE TABLE public.ai_knowledge_document (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    knowledge_base_id character varying(64) NOT NULL,
    document_title character varying(256) NOT NULL,
    source_type character varying(16) DEFAULT 'MANUAL'::character varying NOT NULL,
    source_url character varying(1000),
    source_file_id character varying(64),
    source_file_name character varying(512),
    mime_type character varying(128),
    file_size bigint,
    content text,
    content_hash character varying(64),
    parse_status character varying(16) DEFAULT 'READY'::character varying NOT NULL,
    parse_error character varying(2000),
    chunk_size integer DEFAULT 800 NOT NULL,
    chunk_overlap integer DEFAULT 100 NOT NULL,
    chunk_count integer DEFAULT 0 NOT NULL,
    CONSTRAINT ck_ai_knowledge_document_chunk CHECK ((((chunk_size >= 200) AND (chunk_size <= 4000)) AND ((chunk_overlap >= 0) AND (chunk_overlap <= 1000)) AND (chunk_overlap < chunk_size))),
    CONSTRAINT ck_ai_knowledge_document_source CHECK (((source_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'FILE'::character varying, 'URL'::character varying])::text[]))),
    CONSTRAINT ck_ai_knowledge_document_status CHECK (((parse_status)::text = ANY ((ARRAY['PENDING'::character varying, 'READY'::character varying, 'FAILED'::character varying])::text[])))
);

COMMENT ON TABLE public.ai_knowledge_document IS '知识库文档表';

COMMENT ON COLUMN public.ai_knowledge_document.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_document.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_document.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_document.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_document.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_document.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_document.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_document.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_document.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_document.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_document.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_document.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_document.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_document.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_document.knowledge_base_id IS '所属知识库ID';

COMMENT ON COLUMN public.ai_knowledge_document.document_title IS '文档标题';

COMMENT ON COLUMN public.ai_knowledge_document.source_type IS '来源类型：MANUAL手工、FILE文件、URL链接';

COMMENT ON COLUMN public.ai_knowledge_document.source_url IS '外部来源链接';

COMMENT ON COLUMN public.ai_knowledge_document.source_file_id IS '系统对象存储文件ID';

COMMENT ON COLUMN public.ai_knowledge_document.source_file_name IS '原始文件名';

COMMENT ON COLUMN public.ai_knowledge_document.mime_type IS '文件MIME类型';

COMMENT ON COLUMN public.ai_knowledge_document.file_size IS '文件字节数';

COMMENT ON COLUMN public.ai_knowledge_document.content IS '解析后的权威文本正文';

COMMENT ON COLUMN public.ai_knowledge_document.content_hash IS '规范化正文SHA-256摘要';

COMMENT ON COLUMN public.ai_knowledge_document.parse_status IS '解析状态：PENDING、READY、FAILED';

COMMENT ON COLUMN public.ai_knowledge_document.parse_error IS '解析失败原因';

COMMENT ON COLUMN public.ai_knowledge_document.chunk_size IS '目标分块字符数';

COMMENT ON COLUMN public.ai_knowledge_document.chunk_overlap IS '相邻分块重叠字符数';

COMMENT ON COLUMN public.ai_knowledge_document.chunk_count IS '有效分块数量';

CREATE TABLE public.ai_knowledge_entity (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    knowledge_base_id character varying(64) NOT NULL,
    entity_type character varying(64) NOT NULL,
    entity_name character varying(256) NOT NULL,
    canonical_name character varying(256) NOT NULL,
    description character varying(2000),
    source_document_id character varying(64),
    source_chunk_id character varying(64)
);

COMMENT ON TABLE public.ai_knowledge_entity IS '知识图谱实体表';

COMMENT ON COLUMN public.ai_knowledge_entity.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_entity.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_entity.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_entity.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_entity.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_entity.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_entity.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_entity.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_entity.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_entity.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_entity.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_entity.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_entity.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_entity.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_entity.knowledge_base_id IS '所属知识库ID';

COMMENT ON COLUMN public.ai_knowledge_entity.entity_type IS '实体类型，例如资源、人员、制度、组织';

COMMENT ON COLUMN public.ai_knowledge_entity.entity_name IS '实体展示名称';

COMMENT ON COLUMN public.ai_knowledge_entity.canonical_name IS '实体规范名称，用于检索和去重';

COMMENT ON COLUMN public.ai_knowledge_entity.description IS '实体说明';

COMMENT ON COLUMN public.ai_knowledge_entity.source_document_id IS '来源文档ID';

COMMENT ON COLUMN public.ai_knowledge_entity.source_chunk_id IS '来源分块ID';

CREATE TABLE public.ai_knowledge_relation (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    knowledge_base_id character varying(64) NOT NULL,
    source_entity_id character varying(64) NOT NULL,
    relation_type character varying(128) NOT NULL,
    target_entity_id character varying(64) NOT NULL,
    description character varying(2000),
    confidence numeric(5,4) DEFAULT 1.0000 NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    source_document_id character varying(64),
    source_chunk_id character varying(64),
    CONSTRAINT ck_ai_knowledge_relation_confidence CHECK (((confidence >= (0)::numeric) AND (confidence <= (1)::numeric))),
    CONSTRAINT ck_ai_knowledge_relation_self CHECK (((source_entity_id)::text <> (target_entity_id)::text))
);

COMMENT ON TABLE public.ai_knowledge_relation IS '知识图谱关系表';

COMMENT ON COLUMN public.ai_knowledge_relation.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_relation.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_relation.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_relation.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_relation.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_relation.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_relation.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_relation.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_relation.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_relation.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_relation.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_relation.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_relation.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_relation.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_relation.knowledge_base_id IS '所属知识库ID';

COMMENT ON COLUMN public.ai_knowledge_relation.source_entity_id IS '起点实体ID';

COMMENT ON COLUMN public.ai_knowledge_relation.relation_type IS '关系谓词，例如属于、连接、负责、依据';

COMMENT ON COLUMN public.ai_knowledge_relation.target_entity_id IS '终点实体ID';

COMMENT ON COLUMN public.ai_knowledge_relation.description IS '关系说明';

COMMENT ON COLUMN public.ai_knowledge_relation.confidence IS '关系置信度，范围0到1';

COMMENT ON COLUMN public.ai_knowledge_relation.verified IS '是否经人工核验';

COMMENT ON COLUMN public.ai_knowledge_relation.source_document_id IS '关系来源文档ID';

COMMENT ON COLUMN public.ai_knowledge_relation.source_chunk_id IS '关系来源分块ID';

CREATE TABLE public.ai_knowledge_retrieval (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    knowledge_base_id character varying(64),
    session_id character varying(64),
    user_id character varying(64) NOT NULL,
    query_text text NOT NULL,
    expanded_query text,
    assembled_context text,
    matched_count integer DEFAULT 0 NOT NULL,
    latency_ms bigint DEFAULT 0 NOT NULL,
    retrieval_status character varying(16) DEFAULT 'SUCCESS'::character varying NOT NULL,
    error_message character varying(2000),
    CONSTRAINT ck_ai_knowledge_retrieval_count CHECK ((matched_count >= 0)),
    CONSTRAINT ck_ai_knowledge_retrieval_latency CHECK ((latency_ms >= 0)),
    CONSTRAINT ck_ai_knowledge_retrieval_status CHECK (((retrieval_status)::text = ANY ((ARRAY['SUCCESS'::character varying, 'EMPTY'::character varying, 'FAILED'::character varying])::text[])))
);

COMMENT ON TABLE public.ai_knowledge_retrieval IS '知识库检索记录表';

COMMENT ON COLUMN public.ai_knowledge_retrieval.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_retrieval.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_retrieval.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_retrieval.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_retrieval.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_retrieval.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_retrieval.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_retrieval.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_retrieval.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_retrieval.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_retrieval.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_retrieval.knowledge_base_id IS '指定检索的知识库ID；为空表示检索全部可见知识库';

COMMENT ON COLUMN public.ai_knowledge_retrieval.session_id IS '关联的智能体会话标识；该关联不建立数据库外键';

COMMENT ON COLUMN public.ai_knowledge_retrieval.user_id IS '发起检索的用户ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval.query_text IS '原始问题';

COMMENT ON COLUMN public.ai_knowledge_retrieval.expanded_query IS '经标准词和同义词扩展后的检索词';

COMMENT ON COLUMN public.ai_knowledge_retrieval.assembled_context IS '本次实际注入模型的上下文快照';

COMMENT ON COLUMN public.ai_knowledge_retrieval.matched_count IS '命中分块数量';

COMMENT ON COLUMN public.ai_knowledge_retrieval.latency_ms IS '检索耗时毫秒数';

COMMENT ON COLUMN public.ai_knowledge_retrieval.retrieval_status IS '检索状态：SUCCESS、EMPTY、FAILED';

COMMENT ON COLUMN public.ai_knowledge_retrieval.error_message IS '检索失败原因';

CREATE TABLE public.ai_knowledge_retrieval_hit (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    retrieval_id character varying(64) NOT NULL,
    knowledge_base_id character varying(64) NOT NULL,
    document_id character varying(64) NOT NULL,
    chunk_id character varying(64) NOT NULL,
    hit_rank integer NOT NULL,
    score numeric(10,4) NOT NULL,
    matched_term character varying(256),
    CONSTRAINT ck_ai_knowledge_retrieval_hit_rank CHECK ((hit_rank > 0)),
    CONSTRAINT ck_ai_knowledge_retrieval_hit_score CHECK ((score >= (0)::numeric))
);

COMMENT ON TABLE public.ai_knowledge_retrieval_hit IS '知识库检索命中明细表';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.retrieval_id IS '所属检索记录ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.knowledge_base_id IS '命中知识库ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.document_id IS '命中文档ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.chunk_id IS '命中分块ID';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.hit_rank IS '命中排名，从1开始';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.score IS '命中综合得分';

COMMENT ON COLUMN public.ai_knowledge_retrieval_hit.matched_term IS '主要命中词';

CREATE TABLE public.ai_knowledge_synonym (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    knowledge_base_id character varying(64) NOT NULL,
    term_id character varying(64) NOT NULL,
    alias character varying(256) NOT NULL,
    relation_type character varying(24) DEFAULT 'SYNONYM'::character varying NOT NULL,
    weight numeric(5,4) DEFAULT 1.0000 NOT NULL,
    CONSTRAINT ck_ai_knowledge_synonym_relation CHECK (((relation_type)::text = ANY ((ARRAY['SYNONYM'::character varying, 'NEAR_SYNONYM'::character varying, 'ABBREVIATION'::character varying])::text[]))),
    CONSTRAINT ck_ai_knowledge_synonym_weight CHECK (((weight >= (0)::numeric) AND (weight <= (1)::numeric)))
);

COMMENT ON TABLE public.ai_knowledge_synonym IS '知识库同义词近义词表';

COMMENT ON COLUMN public.ai_knowledge_synonym.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_synonym.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_synonym.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_synonym.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_synonym.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_synonym.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_synonym.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_synonym.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_synonym.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_synonym.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_synonym.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_synonym.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_synonym.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_synonym.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_synonym.knowledge_base_id IS '所属知识库ID';

COMMENT ON COLUMN public.ai_knowledge_synonym.term_id IS '所属标准词ID';

COMMENT ON COLUMN public.ai_knowledge_synonym.alias IS '同义词、近义词或缩写';

COMMENT ON COLUMN public.ai_knowledge_synonym.relation_type IS '词汇关系：SYNONYM、NEAR_SYNONYM、ABBREVIATION';

COMMENT ON COLUMN public.ai_knowledge_synonym.weight IS '查询扩展权重，范围0到1';

CREATE TABLE public.ai_knowledge_term (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    dept_id character varying(64),
    dept_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    remark character varying(500),
    status character varying(20) DEFAULT '1'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT now() NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    knowledge_base_id character varying(64) NOT NULL,
    canonical_term character varying(256) NOT NULL,
    term_type character varying(64),
    definition character varying(2000)
);

COMMENT ON TABLE public.ai_knowledge_term IS '知识库标准词表';

COMMENT ON COLUMN public.ai_knowledge_term.id IS '主键ID';

COMMENT ON COLUMN public.ai_knowledge_term.tenant_id IS '租户ID，所有查询和关联的第一隔离边界';

COMMENT ON COLUMN public.ai_knowledge_term.dept_id IS '部门ID，部门专属数据的授权依据';

COMMENT ON COLUMN public.ai_knowledge_term.dept_name IS '部门展示名称，不参与授权和唯一性判断';

COMMENT ON COLUMN public.ai_knowledge_term.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_knowledge_term.remark IS '备注';

COMMENT ON COLUMN public.ai_knowledge_term.status IS '通用状态';

COMMENT ON COLUMN public.ai_knowledge_term.sort_order IS '排序号，数值越小越靠前';

COMMENT ON COLUMN public.ai_knowledge_term.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_knowledge_term.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_knowledge_term.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_knowledge_term.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_knowledge_term.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_knowledge_term.deleted IS '逻辑删除标记';

COMMENT ON COLUMN public.ai_knowledge_term.knowledge_base_id IS '所属知识库ID';

COMMENT ON COLUMN public.ai_knowledge_term.canonical_term IS '标准词';

COMMENT ON COLUMN public.ai_knowledge_term.term_type IS '词汇类型';

COMMENT ON COLUMN public.ai_knowledge_term.definition IS '标准词定义';

CREATE TABLE public.ai_skill (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp(6) without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp(6) without time zone,
    version integer,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    action_prompt text NOT NULL,
    action_title character varying(128) NOT NULL,
    action_type character varying(32) NOT NULL,
    description character varying(500) NOT NULL,
    jump_type character varying(20),
    jump_url character varying(500),
    owner_id character varying(64),
    pinned boolean NOT NULL,
    terminal_type character varying(20) NOT NULL,
    skill_type character varying(20) NOT NULL,
    display_mode character varying(20),
    weight integer DEFAULT 50 NOT NULL,
    scope_type_code character varying(32) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    sort_order integer DEFAULT 100 NOT NULL,
    CONSTRAINT ck_ai_skill_type CHECK (skill_type IN ('system', 'dept', 'personnel')),
    CONSTRAINT ck_ai_skill_terminal CHECK (terminal_type IN ('website', 'management')),
    CONSTRAINT ck_ai_skill_action CHECK (action_type IN ('presetPrompt', 'qaContinue', 'qaNoRepeat', 'jump', 'dataList')),
    CONSTRAINT ck_ai_skill_jump CHECK (jump_type IS NULL OR jump_type IN ('internal', 'external')),
    CONSTRAINT ck_ai_skill_display CHECK (display_mode IS NULL OR display_mode IN ('prompt', 'chat', 'list', 'jump')),
    CONSTRAINT ck_ai_skill_jump_target CHECK (
        (action_type = 'jump' AND jump_type IS NOT NULL AND jump_url IS NOT NULL)
        OR (action_type <> 'jump' AND jump_type IS NULL AND jump_url IS NULL)
    )
);

COMMENT ON TABLE public.ai_skill IS 'AI技能表';

COMMENT ON COLUMN public.ai_skill.id IS '主键';

COMMENT ON COLUMN public.ai_skill.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_skill.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_skill.deleted IS '软删除标志';

COMMENT ON COLUMN public.ai_skill.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_skill.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_skill.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_skill.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_skill.remark IS '备注';

COMMENT ON COLUMN public.ai_skill.status IS '状态';

COMMENT ON COLUMN public.ai_skill.action_prompt IS '动作提示词';

COMMENT ON COLUMN public.ai_skill.action_title IS '动作标题';

COMMENT ON COLUMN public.ai_skill.action_type IS '动作类型';

COMMENT ON COLUMN public.ai_skill.description IS '动作描述';

COMMENT ON COLUMN public.ai_skill.jump_type IS '跳转类型';

COMMENT ON COLUMN public.ai_skill.jump_url IS '跳转地址';

COMMENT ON COLUMN public.ai_skill.owner_id IS '所属ID';

COMMENT ON COLUMN public.ai_skill.pinned IS '是否置顶';

COMMENT ON COLUMN public.ai_skill.terminal_type IS '端类型：website=用户端，management=管理端';

COMMENT ON COLUMN public.ai_skill.skill_type IS '来源类型：个人personnel、系统system、部门dept';

COMMENT ON COLUMN public.ai_skill.display_mode IS '前端展示模式';

COMMENT ON COLUMN public.ai_skill.weight IS '展示权重，同排序值时数值越大越靠前';

COMMENT ON COLUMN public.ai_skill.scope_type_code IS '作用域类型：GENERAL=通用，KNOWLEDGE=知识库';

COMMENT ON COLUMN public.ai_skill.tenant_id IS '租户ID';

COMMENT ON COLUMN public.ai_skill.sort_order IS '显示排序，数值越小越靠前';

CREATE TABLE public.ai_skill_agent_relation (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    skill_id character varying(64) NOT NULL,
    agent_id character varying(64) NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    status character varying(20),
    sort_order integer,
    remark character varying(500),
    create_by character varying(64),
    create_time timestamp without time zone,
    update_by character varying(64),
    update_time timestamp without time zone,
    version integer,
    CONSTRAINT ck_ai_skill_agent_agent CHECK ((NULLIF(btrim((agent_id)::text), ''::text) IS NOT NULL)),
    CONSTRAINT ck_ai_skill_agent_skill CHECK ((NULLIF(btrim((skill_id)::text), ''::text) IS NOT NULL)),
    CONSTRAINT ck_ai_skill_agent_tenant CHECK ((NULLIF(btrim((tenant_id)::text), ''::text) IS NOT NULL))
);

COMMENT ON TABLE public.ai_skill_agent_relation IS 'AI技能与智能体关系表';

COMMENT ON COLUMN public.ai_skill_agent_relation.id IS '主键';

COMMENT ON COLUMN public.ai_skill_agent_relation.tenant_id IS '租户ID';

COMMENT ON COLUMN public.ai_skill_agent_relation.skill_id IS '技能ID';

COMMENT ON COLUMN public.ai_skill_agent_relation.agent_id IS '智能体ID';

COMMENT ON COLUMN public.ai_skill_agent_relation.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_skill_agent_relation.deleted IS '软删除标志';

COMMENT ON COLUMN public.ai_skill_agent_relation.status IS '状态';

COMMENT ON COLUMN public.ai_skill_agent_relation.sort_order IS '排序号';

COMMENT ON COLUMN public.ai_skill_agent_relation.remark IS '备注';

COMMENT ON COLUMN public.ai_skill_agent_relation.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_skill_agent_relation.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_skill_agent_relation.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_skill_agent_relation.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_skill_agent_relation.version IS '乐观锁版本号';

CREATE TABLE public.ai_skill_scope_type (
    id character varying(64) NOT NULL,
    create_by character varying(64),
    create_time timestamp without time zone,
    deleted boolean NOT NULL,
    update_by character varying(64),
    update_time timestamp without time zone,
    version integer,
    enabled boolean,
    remark character varying(500),
    status character varying(20),
    type_code character varying(32) NOT NULL,
    type_name character varying(64) NOT NULL,
    scope_tag character varying(32),
    display_in_composer boolean NOT NULL,
    default_type boolean NOT NULL,
    weight integer NOT NULL,
    tenant_id character varying(64) NOT NULL
);

COMMENT ON TABLE public.ai_skill_scope_type IS 'AI技能作用域类型表';

COMMENT ON COLUMN public.ai_skill_scope_type.id IS '主键';

COMMENT ON COLUMN public.ai_skill_scope_type.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_skill_scope_type.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_skill_scope_type.deleted IS '软删除标志';

COMMENT ON COLUMN public.ai_skill_scope_type.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_skill_scope_type.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_skill_scope_type.version IS '乐观锁版本号';

COMMENT ON COLUMN public.ai_skill_scope_type.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_skill_scope_type.remark IS '备注';

COMMENT ON COLUMN public.ai_skill_scope_type.status IS '状态';

COMMENT ON COLUMN public.ai_skill_scope_type.type_code IS '类型编码：GENERAL=通用，KNOWLEDGE=知识库';

COMMENT ON COLUMN public.ai_skill_scope_type.type_name IS '类型名称';

COMMENT ON COLUMN public.ai_skill_scope_type.scope_tag IS '输入框作用域标识，例如 @知识库';

COMMENT ON COLUMN public.ai_skill_scope_type.display_in_composer IS '是否在输入框加号下拉展示';

COMMENT ON COLUMN public.ai_skill_scope_type.default_type IS '是否默认类型';

COMMENT ON COLUMN public.ai_skill_scope_type.weight IS '展示权重';

COMMENT ON COLUMN public.ai_skill_scope_type.tenant_id IS '租户ID';

CREATE TABLE public.ai_user_skill_permission (
    id character varying(64) NOT NULL,
    tenant_id character varying(64) NOT NULL,
    user_id character varying(64) NOT NULL,
    skill_id character varying(64) NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    status character varying(20),
    sort_order integer,
    remark character varying(500),
    create_by character varying(64),
    create_time timestamp without time zone,
    update_by character varying(64),
    update_time timestamp without time zone,
    version integer,
    CONSTRAINT ck_ai_user_skill_skill CHECK ((NULLIF(btrim((skill_id)::text), ''::text) IS NOT NULL)),
    CONSTRAINT ck_ai_user_skill_tenant CHECK ((NULLIF(btrim((tenant_id)::text), ''::text) IS NOT NULL)),
    CONSTRAINT ck_ai_user_skill_user CHECK ((NULLIF(btrim((user_id)::text), ''::text) IS NOT NULL))
);

COMMENT ON TABLE public.ai_user_skill_permission IS 'AI用户技能权限表';

COMMENT ON COLUMN public.ai_user_skill_permission.id IS '主键';

COMMENT ON COLUMN public.ai_user_skill_permission.tenant_id IS '租户ID';

COMMENT ON COLUMN public.ai_user_skill_permission.user_id IS '用户ID';

COMMENT ON COLUMN public.ai_user_skill_permission.skill_id IS '技能ID';

COMMENT ON COLUMN public.ai_user_skill_permission.enabled IS '是否启用';

COMMENT ON COLUMN public.ai_user_skill_permission.deleted IS '软删除标志';

COMMENT ON COLUMN public.ai_user_skill_permission.status IS '状态';

COMMENT ON COLUMN public.ai_user_skill_permission.sort_order IS '排序号';

COMMENT ON COLUMN public.ai_user_skill_permission.remark IS '备注';

COMMENT ON COLUMN public.ai_user_skill_permission.create_by IS '创建人ID';

COMMENT ON COLUMN public.ai_user_skill_permission.create_time IS '创建时间';

COMMENT ON COLUMN public.ai_user_skill_permission.update_by IS '更新人ID';

COMMENT ON COLUMN public.ai_user_skill_permission.update_time IS '更新时间';

COMMENT ON COLUMN public.ai_user_skill_permission.version IS '乐观锁版本号';

ALTER TABLE ONLY public.ai_knowledge_base
    ADD CONSTRAINT ai_knowledge_base_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_chunk
    ADD CONSTRAINT ai_knowledge_chunk_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_context
    ADD CONSTRAINT ai_knowledge_context_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_document
    ADD CONSTRAINT ai_knowledge_document_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_entity
    ADD CONSTRAINT ai_knowledge_entity_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_relation
    ADD CONSTRAINT ai_knowledge_relation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_retrieval_hit
    ADD CONSTRAINT ai_knowledge_retrieval_hit_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_retrieval
    ADD CONSTRAINT ai_knowledge_retrieval_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_synonym
    ADD CONSTRAINT ai_knowledge_synonym_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_term
    ADD CONSTRAINT ai_knowledge_term_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_skill_agent_relation
    ADD CONSTRAINT ai_skill_agent_relation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_skill
    ADD CONSTRAINT ai_skill_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_skill_scope_type
    ADD CONSTRAINT ai_skill_scope_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_user_skill_permission
    ADD CONSTRAINT ai_user_skill_permission_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_knowledge_base
    ADD CONSTRAINT uk_ai_knowledge_base_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY public.ai_knowledge_chunk
    ADD CONSTRAINT uk_ai_knowledge_chunk_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY public.ai_knowledge_context
    ADD CONSTRAINT uk_ai_knowledge_context_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY public.ai_knowledge_document
    ADD CONSTRAINT uk_ai_knowledge_document_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY public.ai_knowledge_entity
    ADD CONSTRAINT uk_ai_knowledge_entity_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY public.ai_knowledge_relation
    ADD CONSTRAINT uk_ai_knowledge_relation_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY public.ai_knowledge_retrieval
    ADD CONSTRAINT uk_ai_knowledge_retrieval_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY public.ai_knowledge_synonym
    ADD CONSTRAINT uk_ai_knowledge_synonym_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY public.ai_knowledge_term
    ADD CONSTRAINT uk_ai_knowledge_term_tenant_id UNIQUE (tenant_id, id);

CREATE INDEX idx_ai_knowledge_base_visible ON public.ai_knowledge_base USING btree (tenant_id, scope_type, dept_id, enabled, sort_order) WHERE (deleted = false);

CREATE INDEX idx_ai_knowledge_chunk_lookup ON public.ai_knowledge_chunk USING btree (tenant_id, knowledge_base_id, enabled, create_time DESC) WHERE (deleted = false);

CREATE INDEX idx_ai_knowledge_context_lookup ON public.ai_knowledge_context USING btree (tenant_id, knowledge_base_id, priority, effective_from, effective_to) WHERE ((deleted = false) AND (enabled = true));

CREATE INDEX idx_ai_knowledge_document_base ON public.ai_knowledge_document USING btree (tenant_id, knowledge_base_id, parse_status, create_time DESC) WHERE (deleted = false);

CREATE INDEX idx_ai_knowledge_entity_lookup ON public.ai_knowledge_entity USING btree (tenant_id, knowledge_base_id, lower((canonical_name)::text)) WHERE ((deleted = false) AND (enabled = true));

CREATE INDEX idx_ai_knowledge_relation_source ON public.ai_knowledge_relation USING btree (tenant_id, source_entity_id) WHERE ((deleted = false) AND (enabled = true));

CREATE INDEX idx_ai_knowledge_relation_target ON public.ai_knowledge_relation USING btree (tenant_id, target_entity_id) WHERE ((deleted = false) AND (enabled = true));

CREATE INDEX idx_ai_knowledge_retrieval_hit_record ON public.ai_knowledge_retrieval_hit USING btree (tenant_id, retrieval_id, hit_rank);

CREATE INDEX idx_ai_knowledge_retrieval_recent ON public.ai_knowledge_retrieval USING btree (tenant_id, dept_id, create_time DESC) WHERE (deleted = false);

CREATE INDEX idx_ai_knowledge_synonym_lookup ON public.ai_knowledge_synonym USING btree (tenant_id, knowledge_base_id, lower((alias)::text)) WHERE ((deleted = false) AND (enabled = true));

CREATE INDEX idx_ai_knowledge_term_lookup ON public.ai_knowledge_term USING btree (tenant_id, knowledge_base_id, canonical_term);

CREATE INDEX idx_ai_skill_agent_agent ON public.ai_skill_agent_relation USING btree (tenant_id, agent_id) WHERE (deleted = false);

CREATE INDEX idx_ai_skill_display_order ON public.ai_skill USING btree (tenant_id, terminal_type, pinned DESC, sort_order, weight DESC) WHERE ((deleted = false) AND (enabled = true));

CREATE INDEX idx_ai_skill_pinned ON public.ai_skill USING btree (pinned);

CREATE INDEX idx_ai_skill_scope_type ON public.ai_skill USING btree (scope_type_code);

CREATE INDEX idx_ai_skill_scope_type_tenant_display ON public.ai_skill_scope_type USING btree (tenant_id, display_in_composer, enabled) WHERE (deleted = false);

CREATE INDEX idx_ai_skill_tenant_owner ON public.ai_skill USING btree (tenant_id, skill_type, owner_id) WHERE (deleted = false);

CREATE INDEX idx_ai_skill_terminal ON public.ai_skill USING btree (terminal_type);

CREATE INDEX idx_ai_skill_type_owner ON public.ai_skill USING btree (skill_type, owner_id);

CREATE INDEX idx_ai_user_skill_skill ON public.ai_user_skill_permission USING btree (tenant_id, skill_id) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_base_code_active ON public.ai_knowledge_base USING btree (tenant_id, lower((knowledge_code)::text)) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_base_name_scope_active ON public.ai_knowledge_base USING btree (tenant_id, scope_type, COALESCE(dept_id, ''::character varying), lower((knowledge_name)::text)) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_chunk_order_active ON public.ai_knowledge_chunk USING btree (tenant_id, document_id, chunk_index) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_context_active ON public.ai_knowledge_context USING btree (tenant_id, knowledge_base_id, lower((context_name)::text)) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_document_hash_active ON public.ai_knowledge_document USING btree (tenant_id, knowledge_base_id, content_hash) WHERE ((deleted = false) AND (content_hash IS NOT NULL));

CREATE UNIQUE INDEX uk_ai_knowledge_entity_name_active ON public.ai_knowledge_entity USING btree (tenant_id, knowledge_base_id, lower((entity_type)::text), lower((canonical_name)::text)) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_relation_active ON public.ai_knowledge_relation USING btree (tenant_id, knowledge_base_id, source_entity_id, lower((relation_type)::text), target_entity_id) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_retrieval_hit_rank ON public.ai_knowledge_retrieval_hit USING btree (tenant_id, retrieval_id, hit_rank) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_synonym_active ON public.ai_knowledge_synonym USING btree (tenant_id, term_id, lower((alias)::text)) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_knowledge_term_active ON public.ai_knowledge_term USING btree (tenant_id, knowledge_base_id, lower((canonical_term)::text)) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_skill_agent_active ON public.ai_skill_agent_relation USING btree (tenant_id, skill_id) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_skill_scope_type_code_active ON public.ai_skill_scope_type USING btree (tenant_id, type_code) WHERE (deleted = false);

CREATE UNIQUE INDEX uk_ai_skill_system_scope_active ON public.ai_skill USING btree (tenant_id, terminal_type, scope_type_code) WHERE ((deleted = false) AND ((skill_type)::text = 'system'::text));

CREATE UNIQUE INDEX uk_ai_user_skill_active ON public.ai_user_skill_permission USING btree (tenant_id, user_id, skill_id) WHERE (deleted = false);

ALTER TABLE ONLY public.ai_knowledge_chunk
    ADD CONSTRAINT fk_ai_knowledge_chunk_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_chunk
    ADD CONSTRAINT fk_ai_knowledge_chunk_document FOREIGN KEY (tenant_id, document_id) REFERENCES public.ai_knowledge_document(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_context
    ADD CONSTRAINT fk_ai_knowledge_context_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_document
    ADD CONSTRAINT fk_ai_knowledge_document_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_entity
    ADD CONSTRAINT fk_ai_knowledge_entity_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_entity
    ADD CONSTRAINT fk_ai_knowledge_entity_chunk FOREIGN KEY (tenant_id, source_chunk_id) REFERENCES public.ai_knowledge_chunk(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.ai_knowledge_entity
    ADD CONSTRAINT fk_ai_knowledge_entity_document FOREIGN KEY (tenant_id, source_document_id) REFERENCES public.ai_knowledge_document(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.ai_knowledge_relation
    ADD CONSTRAINT fk_ai_knowledge_relation_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_relation
    ADD CONSTRAINT fk_ai_knowledge_relation_chunk FOREIGN KEY (tenant_id, source_chunk_id) REFERENCES public.ai_knowledge_chunk(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.ai_knowledge_relation
    ADD CONSTRAINT fk_ai_knowledge_relation_document FOREIGN KEY (tenant_id, source_document_id) REFERENCES public.ai_knowledge_document(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.ai_knowledge_relation
    ADD CONSTRAINT fk_ai_knowledge_relation_source FOREIGN KEY (tenant_id, source_entity_id) REFERENCES public.ai_knowledge_entity(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.ai_knowledge_relation
    ADD CONSTRAINT fk_ai_knowledge_relation_target FOREIGN KEY (tenant_id, target_entity_id) REFERENCES public.ai_knowledge_entity(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.ai_knowledge_retrieval
    ADD CONSTRAINT fk_ai_knowledge_retrieval_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.ai_knowledge_retrieval_hit
    ADD CONSTRAINT fk_ai_knowledge_retrieval_hit_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_retrieval_hit
    ADD CONSTRAINT fk_ai_knowledge_retrieval_hit_chunk FOREIGN KEY (tenant_id, chunk_id) REFERENCES public.ai_knowledge_chunk(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_retrieval_hit
    ADD CONSTRAINT fk_ai_knowledge_retrieval_hit_document FOREIGN KEY (tenant_id, document_id) REFERENCES public.ai_knowledge_document(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_retrieval_hit
    ADD CONSTRAINT fk_ai_knowledge_retrieval_hit_retrieval FOREIGN KEY (tenant_id, retrieval_id) REFERENCES public.ai_knowledge_retrieval(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_synonym
    ADD CONSTRAINT fk_ai_knowledge_synonym_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_synonym
    ADD CONSTRAINT fk_ai_knowledge_synonym_term FOREIGN KEY (tenant_id, term_id) REFERENCES public.ai_knowledge_term(tenant_id, id) ON DELETE CASCADE;

ALTER TABLE ONLY public.ai_knowledge_term
    ADD CONSTRAINT fk_ai_knowledge_term_base FOREIGN KEY (tenant_id, knowledge_base_id) REFERENCES public.ai_knowledge_base(tenant_id, id) ON DELETE CASCADE;

COMMENT ON COLUMN public.sse_connection_record.id IS '主键';
COMMENT ON COLUMN public.sse_connection_record.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.sse_connection_record.create_time IS '创建时间';
COMMENT ON COLUMN public.sse_connection_record.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.sse_connection_record.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.sse_connection_record.update_time IS '最后修改时间';
COMMENT ON COLUMN public.sse_connection_record.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sse_connection_record.enabled IS '是否启用';
COMMENT ON COLUMN public.sse_connection_record.remark IS '备注';
COMMENT ON COLUMN public.sse_connection_record.sort_order IS '排序号';
COMMENT ON COLUMN public.sse_connection_record.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.sse_connection_record.client_ip IS '客户端 IP 地址';
COMMENT ON COLUMN public.sse_connection_record.connect_time IS '连接建立时间';
COMMENT ON COLUMN public.sse_connection_record.connection_id IS '连接唯一标识';
COMMENT ON COLUMN public.sse_connection_record.connection_status IS '连接状态';
COMMENT ON COLUMN public.sse_connection_record.disconnect_reason IS '连接断开原因';
COMMENT ON COLUMN public.sse_connection_record.disconnect_time IS '连接断开时间';
COMMENT ON COLUMN public.sse_connection_record.duration_seconds IS '连接持续时长，单位为秒';
COMMENT ON COLUMN public.sse_connection_record.server_instance IS '承载连接的服务实例标识';
COMMENT ON COLUMN public.sse_connection_record.user_id IS '连接所属用户标识';

COMMENT ON COLUMN public.sse_push_log.id IS '主键';
COMMENT ON COLUMN public.sse_push_log.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.sse_push_log.create_time IS '创建时间';
COMMENT ON COLUMN public.sse_push_log.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.sse_push_log.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.sse_push_log.update_time IS '最后修改时间';
COMMENT ON COLUMN public.sse_push_log.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sse_push_log.enabled IS '是否启用';
COMMENT ON COLUMN public.sse_push_log.remark IS '备注';
COMMENT ON COLUMN public.sse_push_log.sort_order IS '排序号';
COMMENT ON COLUMN public.sse_push_log.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.sse_push_log.fail_reason IS '推送失败或跳过原因';
COMMENT ON COLUMN public.sse_push_log.message_id IS '消息队列消息标识';
COMMENT ON COLUMN public.sse_push_log.message_type IS '消息类型';
COMMENT ON COLUMN public.sse_push_log.push_content IS '推送内容 JSON';
COMMENT ON COLUMN public.sse_push_log.push_status IS '推送状态';
COMMENT ON COLUMN public.sse_push_log.push_time IS '推送时间';
COMMENT ON COLUMN public.sse_push_log.push_title IS '消息标题';
COMMENT ON COLUMN public.sse_push_log.retry_count IS '重试次数';
COMMENT ON COLUMN public.sse_push_log.target_type IS '推送目标类型';
COMMENT ON COLUMN public.sse_push_log.user_id IS '单个目标用户标识';
COMMENT ON COLUMN public.sse_push_log.user_ids IS '多个目标用户标识的 JSON 数组';

COMMENT ON COLUMN public.sse_op_log.id IS '主键';
COMMENT ON COLUMN public.sse_op_log.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.sse_op_log.create_time IS '创建时间';
COMMENT ON COLUMN public.sse_op_log.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.sse_op_log.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.sse_op_log.update_time IS '最后修改时间';
COMMENT ON COLUMN public.sse_op_log.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sse_op_log.enabled IS '是否启用';
COMMENT ON COLUMN public.sse_op_log.remark IS '备注';
COMMENT ON COLUMN public.sse_op_log.sort_order IS '排序号';
COMMENT ON COLUMN public.sse_op_log.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.sse_op_log.client_ip IS '客户端 IP 地址';
COMMENT ON COLUMN public.sse_op_log.location IS '客户端地理位置';
COMMENT ON COLUMN public.sse_op_log.status IS '日志处理状态';
COMMENT ON COLUMN public.sse_op_log.log_time IS '日志发生时间';
COMMENT ON COLUMN public.sse_op_log.message IS '日志消息';
COMMENT ON COLUMN public.sse_op_log.trace_id IS '链路追踪标识';
COMMENT ON COLUMN public.sse_op_log.user_id IS '操作用户标识';
COMMENT ON COLUMN public.sse_op_log.username IS '操作用户名';
COMMENT ON COLUMN public.sse_op_log.connection_id IS 'SSE 连接标识';
COMMENT ON COLUMN public.sse_op_log.content IS '推送内容';
COMMENT ON COLUMN public.sse_op_log.cost_ms IS '执行耗时，单位为毫秒';
COMMENT ON COLUMN public.sse_op_log.description IS '操作描述';
COMMENT ON COLUMN public.sse_op_log.fail_reason IS '失败原因';
COMMENT ON COLUMN public.sse_op_log.message_type IS '消息类型';
COMMENT ON COLUMN public.sse_op_log.operation_id IS '关联操作标识';
COMMENT ON COLUMN public.sse_op_log.operation_type IS '操作类型';
COMMENT ON COLUMN public.sse_op_log.target_type IS '推送目标类型';
COMMENT ON COLUMN public.file_edit_lock.id IS '主键';
COMMENT ON COLUMN public.file_edit_lock.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.file_edit_lock.create_time IS '创建时间';
COMMENT ON COLUMN public.file_edit_lock.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.file_edit_lock.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.file_edit_lock.update_time IS '最后修改时间';
COMMENT ON COLUMN public.file_edit_lock.version IS '乐观锁版本号';
COMMENT ON COLUMN public.file_edit_lock.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.file_edit_lock.enabled IS '是否启用';
COMMENT ON COLUMN public.file_edit_lock.remark IS '备注';
COMMENT ON COLUMN public.file_edit_lock.status IS '业务状态';
COMMENT ON COLUMN public.file_edit_lock.node_id IS '被锁定的文件节点标识';
COMMENT ON COLUMN public.file_edit_lock.owner_user_id IS '编辑锁持有用户标识';
COMMENT ON COLUMN public.file_edit_lock.token_hash IS '编辑锁令牌哈希值';
COMMENT ON COLUMN public.file_edit_lock.expires_at IS '编辑锁过期时间';

COMMENT ON COLUMN public.file_grant.id IS '主键';
COMMENT ON COLUMN public.file_grant.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.file_grant.create_time IS '创建时间';
COMMENT ON COLUMN public.file_grant.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.file_grant.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.file_grant.update_time IS '最后修改时间';
COMMENT ON COLUMN public.file_grant.version IS '乐观锁版本号';
COMMENT ON COLUMN public.file_grant.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.file_grant.enabled IS '是否启用';
COMMENT ON COLUMN public.file_grant.remark IS '备注';
COMMENT ON COLUMN public.file_grant.status IS '业务状态';
COMMENT ON COLUMN public.file_grant.space_id IS '所属文件空间标识';
COMMENT ON COLUMN public.file_grant.node_id IS '授权文件节点标识；为空时作用于整个空间';
COMMENT ON COLUMN public.file_grant.principal_type IS '授权主体类型';
COMMENT ON COLUMN public.file_grant.principal_id IS '授权主体标识';
COMMENT ON COLUMN public.file_grant.grant_role IS '授予的文件访问角色';
COMMENT ON COLUMN public.file_grant.inherited IS '是否继承到子节点';
COMMENT ON COLUMN public.file_grant.expires_at IS '授权过期时间';

COMMENT ON COLUMN public.file_node.id IS '主键';
COMMENT ON COLUMN public.file_node.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.file_node.create_time IS '创建时间';
COMMENT ON COLUMN public.file_node.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.file_node.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.file_node.update_time IS '最后修改时间';
COMMENT ON COLUMN public.file_node.version IS '乐观锁版本号';
COMMENT ON COLUMN public.file_node.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.file_node.enabled IS '是否启用';
COMMENT ON COLUMN public.file_node.remark IS '备注';
COMMENT ON COLUMN public.file_node.status IS '业务状态';
COMMENT ON COLUMN public.file_node.space_id IS '所属文件空间标识';
COMMENT ON COLUMN public.file_node.parent_id IS '父文件节点标识';
COMMENT ON COLUMN public.file_node.node_type IS '节点类型';
COMMENT ON COLUMN public.file_node.node_name IS '节点名称';
COMMENT ON COLUMN public.file_node.display_path IS '节点展示路径';
COMMENT ON COLUMN public.file_node.current_version_id IS '当前文件版本标识';
COMMENT ON COLUMN public.file_node.node_state IS '节点状态';
COMMENT ON COLUMN public.file_node.trashed_at IS '移入回收站时间';
COMMENT ON COLUMN public.file_node.trashed_by IS '执行回收操作的用户标识';
COMMENT ON COLUMN public.file_node.business_type IS '关联业务类型';
COMMENT ON COLUMN public.file_node.business_id IS '关联业务标识';

COMMENT ON COLUMN public.file_operation_log.id IS '主键';
COMMENT ON COLUMN public.file_operation_log.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.file_operation_log.create_time IS '创建时间';
COMMENT ON COLUMN public.file_operation_log.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.file_operation_log.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.file_operation_log.update_time IS '最后修改时间';
COMMENT ON COLUMN public.file_operation_log.version IS '乐观锁版本号';
COMMENT ON COLUMN public.file_operation_log.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.file_operation_log.enabled IS '是否启用';
COMMENT ON COLUMN public.file_operation_log.remark IS '备注';
COMMENT ON COLUMN public.file_operation_log.status IS '业务状态';
COMMENT ON COLUMN public.file_operation_log.operator_user_id IS '操作用户标识';
COMMENT ON COLUMN public.file_operation_log.operation IS '文件操作类型';
COMMENT ON COLUMN public.file_operation_log.space_id IS '所属文件空间标识';
COMMENT ON COLUMN public.file_operation_log.node_id IS '操作的文件节点标识';
COMMENT ON COLUMN public.file_operation_log.detail IS '操作详情';

COMMENT ON COLUMN public.file_space.id IS '主键';
COMMENT ON COLUMN public.file_space.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.file_space.create_time IS '创建时间';
COMMENT ON COLUMN public.file_space.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.file_space.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.file_space.update_time IS '最后修改时间';
COMMENT ON COLUMN public.file_space.version IS '乐观锁版本号';
COMMENT ON COLUMN public.file_space.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.file_space.enabled IS '是否启用';
COMMENT ON COLUMN public.file_space.remark IS '备注';
COMMENT ON COLUMN public.file_space.status IS '业务状态';
COMMENT ON COLUMN public.file_space.space_code IS '租户内唯一的文件空间编码';
COMMENT ON COLUMN public.file_space.space_name IS '文件空间名称';
COMMENT ON COLUMN public.file_space.space_type IS '文件空间类型';
COMMENT ON COLUMN public.file_space.owner_user_id IS '个人空间所属用户标识';
COMMENT ON COLUMN public.file_space.owner_dept_id IS '部门空间所属部门标识';
COMMENT ON COLUMN public.file_space.quota_bytes IS '空间容量配额，单位为字节';
COMMENT ON COLUMN public.file_space.used_bytes IS '已使用容量，单位为字节';

COMMENT ON COLUMN public.file_version.id IS '主键';
COMMENT ON COLUMN public.file_version.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.file_version.create_time IS '创建时间';
COMMENT ON COLUMN public.file_version.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.file_version.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.file_version.update_time IS '最后修改时间';
COMMENT ON COLUMN public.file_version.version IS '乐观锁版本号';
COMMENT ON COLUMN public.file_version.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.file_version.enabled IS '是否启用';
COMMENT ON COLUMN public.file_version.remark IS '备注';
COMMENT ON COLUMN public.file_version.status IS '业务状态';
COMMENT ON COLUMN public.file_version.node_id IS '所属文件节点标识';
COMMENT ON COLUMN public.file_version.version_no IS '文件版本号';
COMMENT ON COLUMN public.file_version.object_key IS '对象存储键';
COMMENT ON COLUMN public.file_version.original_name IS '原始文件名';
COMMENT ON COLUMN public.file_version.content_type IS '文件 MIME 类型';
COMMENT ON COLUMN public.file_version.size_bytes IS '文件大小，单位为字节';
COMMENT ON COLUMN public.file_version.sha256 IS '文件内容 SHA-256 摘要';

COMMENT ON COLUMN public.scheduler_task.id IS '主键';
COMMENT ON COLUMN public.scheduler_task.create_by IS '创建人用户标识';
COMMENT ON COLUMN public.scheduler_task.create_time IS '创建时间';
COMMENT ON COLUMN public.scheduler_task.deleted IS '逻辑删除标志';
COMMENT ON COLUMN public.scheduler_task.update_by IS '最后修改人用户标识';
COMMENT ON COLUMN public.scheduler_task.update_time IS '最后修改时间';
COMMENT ON COLUMN public.scheduler_task.version IS '乐观锁版本号';
COMMENT ON COLUMN public.scheduler_task.tenant_id IS '所属租户标识';
COMMENT ON COLUMN public.scheduler_task.enabled IS '是否启用';
COMMENT ON COLUMN public.scheduler_task.remark IS '备注';
COMMENT ON COLUMN public.scheduler_task.status IS '业务状态';
COMMENT ON COLUMN public.scheduler_task.task_code IS '租户内唯一的调度任务编码';
COMMENT ON COLUMN public.scheduler_task.task_name IS '调度任务名称';
COMMENT ON COLUMN public.scheduler_task.time_expression_type IS '时间表达式类型';
COMMENT ON COLUMN public.scheduler_task.time_expression IS '时间表达式';
COMMENT ON COLUMN public.scheduler_task.job_parameters IS '任务参数 JSON';
COMMENT ON COLUMN public.scheduler_task.max_instance_num IS '同时存在的最大实例数';
COMMENT ON COLUMN public.scheduler_task.concurrency IS '单个 Worker 的并发度';
COMMENT ON COLUMN public.scheduler_task.instance_time_limit IS '单个实例超时时间，单位为毫秒';
COMMENT ON COLUMN public.scheduler_task.instance_retry_num IS '实例级最大重试次数';
COMMENT ON COLUMN public.scheduler_task.task_retry_num IS '任务级最大重试次数';
COMMENT ON COLUMN public.scheduler_task.powerjob_job_id IS 'PowerJob 任务标识';
COMMENT ON COLUMN public.scheduler_task.last_sync_time IS '最近一次同步时间';
COMMENT ON COLUMN public.scheduler_task.last_sync_message IS '最近一次同步结果说明';

COMMENT ON COLUMN public.sys_oss_log.file_md5 IS '文件 MD5 摘要';
COMMENT ON COLUMN public.sys_oss_log.stored_name IS '存储文件名';
COMMENT ON COLUMN public.sys_oss_log.upload_status IS '上传状态';
COMMENT ON COLUMN public.sys_oss_log.upload_time IS '上传时间';
COMMENT ON COLUMN public.sys_setting.extensions IS '产品命名空间扩展偏好 JSON';
