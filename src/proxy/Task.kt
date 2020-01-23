package proxy

interface Task {
    fun setData(data: String?)
    fun getCalData(x: Int): Int
}