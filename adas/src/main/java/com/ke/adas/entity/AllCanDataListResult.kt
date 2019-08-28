package com.ke.adas.entity

import bean.OBDAutoCrackElement

interface AllCanDataListResult


data class GetAllCANDataListFinish(val size: Int) : AllCanDataListResult

data class FilterOutChangedDataFinish(val size: Int) : AllCanDataListResult

data class FilterOutFixedDataFinish(val size: Int) : AllCanDataListResult

data class CurBeFilter(val obdAutoCrackElement: OBDAutoCrackElement) : AllCanDataListResult