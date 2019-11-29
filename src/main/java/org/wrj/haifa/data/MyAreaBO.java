package org.wrj.haifa.data;

public class MyAreaBO {

    private static final long serialVersionUID = 1L;

    /**
     * 地区编码
     */
    private Integer id;

    /**
     * 地区全路径 省-市-区
     */
    private String pathCode;

    /**
     * 地区名称
     */
    private String name;

    /**
     * 上级地区编码,引自areaid
     */
    private Integer parentId;

    /**
     * 地区类型（数据字典标识：d_area_type）
     * 1.省级 2.市级 3.区/县级
     */
    private Integer type;

    /**
     * 排序码
     */
    private Integer sortCode;

    /**
     * 国标码
     */
    private String nationalCode;

    /**
     * 创建员工
     */
    private Long staffCreated;

    /**
     * 修改员工
     */
    private Long staffModified;

    /**
     * 是否删除
     */
    private Boolean isDelete;

    /**
     * `region_code` varchar(20) NOT NULL DEFAULT '0' COMMENT '国家规划局区划编码',
     *   `parent_region_code` varchar(20) NOT NULL DEFAULT '0' COMMENT '父级国家规划局区划编码',
     */

    /**
     * 国家规划局区划编码
     */
    private String regionCode;

    /**
     * 父级国家规划局区划编码
     */
    private String parentRegionCode;

    /**
     * 是否显示新增按钮
     */
    private boolean canAdd;

    /**
     * 是否显示删除按钮
     */
    private boolean canDelete;

    /**
     * 是否显示修改按钮
     */
    private boolean canUpdate;

    public boolean isCanAdd() {
        return canAdd;
    }

    public void setCanAdd(boolean canAdd) {
        this.canAdd = canAdd;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean isCanUpdate() {
        return canUpdate;
    }

    public void setCanUpdate(boolean canUpdate) {
        this.canUpdate = canUpdate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getSortCode() {
        return sortCode;
    }

    public void setSortCode(Integer sortCode) {
        this.sortCode = sortCode;
    }

    public String getNationalCode() {
        return nationalCode;
    }

    public void setNationalCode(String nationalCode) {
        this.nationalCode = nationalCode;
    }

    public String getPathCode() {
        return pathCode;
    }

    public void setPathCode(String pathCode) {
        this.pathCode = pathCode;
    }

    public Long getStaffCreated() {
        return staffCreated;
    }

    public void setStaffCreated(Long staffCreated) {
        this.staffCreated = staffCreated;
    }

    public Long getStaffModified() {
        return staffModified;
    }

    public void setStaffModified(Long staffModified) {
        this.staffModified = staffModified;
    }

    public Boolean getDelete() {
        return isDelete;
    }

    public void setDelete(Boolean delete) {
        isDelete = delete;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getParentRegionCode() {
        return parentRegionCode;
    }

    public void setParentRegionCode(String parentRegionCode) {
        this.parentRegionCode = parentRegionCode;
    }

    @Override
    public String toString() {
        return "MyAreaBO{" +
                "id=" + id +
                ", pathCode='" + pathCode + '\'' +
                ", name='" + name + '\'' +
                ", parentId=" + parentId +
                ", type=" + type +
                ", sortCode=" + sortCode +
                ", nationalCode='" + nationalCode + '\'' +
                ", staffCreated=" + staffCreated +
                ", staffModified=" + staffModified +
                ", isDelete=" + isDelete +
                ", regionCode='" + regionCode + '\'' +
                ", parentRegionCode='" + parentRegionCode + '\'' +
                ", canAdd=" + canAdd +
                ", canDelete=" + canDelete +
                ", canUpdate=" + canUpdate +
                '}';
    }
}
