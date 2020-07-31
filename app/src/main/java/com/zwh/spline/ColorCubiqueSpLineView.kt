package com.zwh.spline

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.graphics.Paint.Cap
import android.graphics.Paint.Join.ROUND
import android.graphics.Paint.Style.STROKE
import android.graphics.Path.Direction.CW
import android.graphics.Path.Op.INTERSECT
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import com.zwh.spline.interpolator.SpLineInterpolator
import android.view.MotionEvent
import android.view.View
import com.zwh.spline.utils.DPUtils
import java.util.LinkedList
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * @Author neil
 * @Description 曲线调色 样条曲线
 * @Email 616505546@qq.com
 */
class ColorCubiqueSpLineView @JvmOverloads constructor(
  context: Context?,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  enum class ColorSpLineType{
    RGB,RED,GREEN,BLUE,
  }

  //选中曲线线条颜色
  private var mSelectedCurveLineColor = resources.getColor(R.color.white)
  //未选中曲线线条颜色
  private var mUnSelectedCurveLineColor = resources.getColor(R.color.white)
  //曲线线条粗细
  private var mCurveLineWidth = DPUtils.dpToPixel(context, 1)
  //被选中控制点大小
  private var mSelectedKnotRadius = DPUtils.dpFloatToPixel(context, 6f)
  //曲线控制点默认大小
  private var mDefaultKnotRadius = DPUtils.dpFloatToPixel(context, 4.5f)
  //曲线控制点默认颜色
  private var mDefaultKnotColor = resources.getColor(R.color.white)
  //曲线控制点选中状态颜色
  private var mSelectedKnotColor = resources.getColor(R.color.colorPrimary)
  //控制点边框粗细
  private var mKnotStrokeWidth = DPUtils.dpToPixel(context, 1)
  //控制点外边框颜色
  private var mKnotStrokeColor = resources.getColor(R.color.white)
  //选中控制点外边框颜色
  private var mSelectKnotStrokeColor = resources.getColor(R.color.white)
  //整个View的背景
  private var mBgColor = resources.getColor(R.color.opacity_5_black)
  //背景格子线颜色
  private var mBgLineColor = resources.getColor(R.color.color_33e0e0e0)
  //背景格子线粗细
  private var mBgLineWidth = DPUtils.dpToPixel(context, 0.5f)
  //背景格子数
  private var mBgLineCount = 4
  //控制点最小间距
  private var minKnotsSpacing = mSelectedKnotRadius * 2 - mCurveLineWidth;

  private lateinit var mCurvePaint: Paint
  private lateinit var mKnotPaint: Paint
  private lateinit var mTextPaint: Paint
  private lateinit var mBgPaint: Paint
  private lateinit var mSlideBtnPaint: Paint
  private lateinit var mDeleteBtnPaint: Paint

  private var enableMoveEvent = false
  private var selectNewPoint = false
  private var onGestureIntercept = false
  private var curKnotIdx = -1

  private var curKnotsList: LinkedList<PointF> = LinkedList()
  private var rgbKnotsList: LinkedList<PointF> = LinkedList()
  private var redKnotsList: LinkedList<PointF> = LinkedList()
  private var greenKnotsList: LinkedList<PointF> = LinkedList()
  private var blueKnotsList: LinkedList<PointF> = LinkedList()
  private var curPath = Path()
  private var rgbPath = Path()
  private var redPath = Path()
  private var greenPath = Path()
  private var bluePath = Path()
  private var curRectPath = Path() //记录曲线轨迹的大致范围，用来判断触摸点是否在曲线轨迹附近
  private var curSelectKnotPath = Path()   //当前选中点的范围path，用来判断触摸点是否在选中点
  private val sip = SpLineInterpolator()

  private var mWidth = 0
  private var mHeight = 0
  private var mStartX = 0
  private var mStartY = 0
  private var mEndX = 0
  private var mEndY = 0

  private var mDownX = 0f
  private var mDownY = 0f

  private var mGestureDetector: GestureDetector? = null

  //边界空余大小，防止边界处无法移动控制点
  private val mFixSize = DPUtils.dpToPixel(context, 8)

  private var mCallBack : OnCtrPointsUpdateCallBack?=null

  private var curSpType = ColorSpLineType.RGB

  init {
    reloadPaints()
    mGestureDetector = GestureDetector(context, GestureListener())
  }

  private fun initKontList() {
    resetKnotsList()
    switchSpLineType(curSpType,false)
  }

  private fun resetKnotsList() {
    rgbKnotsList.add(PointF(mStartX.toFloat(), mEndY.toFloat()))
    rgbKnotsList.add(PointF(mEndX.toFloat(), mStartY.toFloat()))
    redKnotsList.add(PointF(mStartX.toFloat(), mEndY.toFloat()))
    redKnotsList.add(PointF(mEndX.toFloat(), mStartY.toFloat()))
    greenKnotsList.add(PointF(mStartX.toFloat(), mEndY.toFloat()))
    greenKnotsList.add(PointF(mEndX.toFloat(), mStartY.toFloat()))
    blueKnotsList.add(PointF(mStartX.toFloat(), mEndY.toFloat()))
    blueKnotsList.add(PointF(mEndX.toFloat(), mStartY.toFloat()))
  }

  fun switchSpLineType(spType : ColorSpLineType, refreshUI : Boolean = true) {
    when (spType) {
      ColorSpLineType.RGB -> {
        curKnotsList = rgbKnotsList
        curPath = rgbPath
        mSelectedCurveLineColor = resources.getColor(R.color.white)
        mUnSelectedCurveLineColor = resources.getColor(R.color.white)
        mDefaultKnotColor = resources.getColor(R.color.white)
        mSelectedKnotColor = resources.getColor(R.color.colorPrimary)
      }
      ColorSpLineType.RED -> {
        curKnotsList = redKnotsList
        curPath = redPath
        mSelectedCurveLineColor = resources.getColor(R.color.color_ff443b)
        mUnSelectedCurveLineColor = resources.getColor(R.color.color_33ff443b)
        mDefaultKnotColor = resources.getColor(R.color.color_ff443b)
        mSelectedKnotColor = resources.getColor(R.color.color_ff443b)
      }
      ColorSpLineType.GREEN -> {
        curKnotsList = greenKnotsList
        curPath = greenPath
        mSelectedCurveLineColor = resources.getColor(R.color.color_84F767)
        mUnSelectedCurveLineColor = resources.getColor(R.color.color_3384F767)
        mDefaultKnotColor = resources.getColor(R.color.color_84F767)
        mSelectedKnotColor = resources.getColor(R.color.color_84F767)
      }
      ColorSpLineType.BLUE -> {
        curKnotsList = blueKnotsList
        curPath = bluePath
        mSelectedCurveLineColor = resources.getColor(R.color.color_3261ff)
        mUnSelectedCurveLineColor = resources.getColor(R.color.color_333261ff)
        mDefaultKnotColor = resources.getColor(R.color.color_3261ff)
        mSelectedKnotColor = resources.getColor(R.color.color_3261ff)
      }
    }
    curSpType = spType
    curKnotIdx = -1
    if (refreshUI){
      invalidate()
    }
  }

  private fun reloadPaints() {
    mBgPaint = Paint()
    mCurvePaint = Paint()
    mKnotPaint = Paint()
    mTextPaint = Paint()
    mSlideBtnPaint = Paint()
    mDeleteBtnPaint = Paint()

    mCurvePaint.flags = mCurvePaint.flags or Paint.ANTI_ALIAS_FLAG
    mCurvePaint.strokeWidth = mCurveLineWidth.toFloat()
    mCurvePaint.isDither = true
    mCurvePaint.style = STROKE
    mCurvePaint.strokeJoin = ROUND
    mCurvePaint.strokeCap = Cap.ROUND
    mCurvePaint.isAntiAlias = true

    mTextPaint.textAlign = Align.CENTER
    mTextPaint.textSize = DPUtils.dpToPixel(context, 9).toFloat()
    mTextPaint.color = resources.getColor(R.color.color_4E4E51)
    mTextPaint.isAntiAlias = true

    mBgPaint.isAntiAlias = true

    mKnotPaint.isAntiAlias = true

    mSlideBtnPaint.isAntiAlias = true
    mSlideBtnPaint.color = resources.getColor(R.color.white)

    mDeleteBtnPaint.isAntiAlias = true
    mDeleteBtnPaint.color = resources.getColor(R.color.color_3f3f3f)
  }

  override fun onSizeChanged(
    w: Int,
    h: Int,
    oldw: Int,
    oldh: Int
  ) {
    super.onSizeChanged(w, h, oldw, oldh)
    rescaleView(w, h)
    initKontList()
    invalidate()
  }

  private fun rescaleView(
    width: Int,
    height: Int
  ) {
    mWidth = width
    mHeight = height
    mStartX = mFixSize
    mStartY = mFixSize
    mEndX = mWidth - mFixSize
    mEndY = mHeight - mFixSize
  }

  override fun draw(canvas: Canvas?) {
    super.draw(canvas)
    drawBg(canvas)
    drawBgLine(canvas)
    drawUnselectedSpLine(canvas)
    drawSelectedSpline(canvas)
    drawCtrlKnot(canvas)
    drawSelectPointText(canvas)
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    val x = limitX(event!!.x)
    val y = limitY(event.y)
    if (event.pointerCount == 1 && mGestureDetector != null) {
      mGestureDetector?.onTouchEvent(event)
    }
    if (onGestureIntercept){
      onGestureIntercept = false
      return true
    }
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        mDownX = x
        mDownY = y
        if (isPointInCurSpPath(x, y)) {
          enableMoveEvent = true
          addNewKnot(x, y)
          refreshCurSelectKnotPath()
        } else {
          curKnotIdx = -1
          enableMoveEvent = false
        }
        invalidate()
      }
      MotionEvent.ACTION_MOVE ->{
        val minMove = sqrt((y - mDownY).toDouble().pow(2) + (x - mDownX).toDouble().pow(2))
        if (minMove <= mDefaultKnotRadius){
          return true
        }
        val limitX = minKnotsSpacing
        val curPoint = curKnotsList.getOrNull(curKnotIdx)
        if (enableMoveEvent && curPoint != null) {
          curPoint.y = y
          if (curKnotIdx >= 1 && x <= curKnotsList[curKnotIdx - 1].x + limitX) {
            curPoint.x = curKnotsList[curKnotIdx - 1].x + limitX
          } else if (curKnotIdx < curKnotsList.size - 1 && x >= curKnotsList[curKnotIdx + 1].x - limitX) {
            curPoint.x = curKnotsList[curKnotIdx + 1].x - limitX
          } else {
            curPoint.x = x
          }
          mCallBack?.onUpdate(getFixArrayPoints(curKnotsList),curSpType)
          invalidate()
        }
      }
      MotionEvent.ACTION_UP -> {
        if (enableMoveEvent){
          mCallBack?.onUpdate(getFixArrayPoints(curKnotsList),curSpType)
        }
        enableMoveEvent = false
      }
    }
    return true
  }

  private fun refreshCurSelectKnotPath() {
    curSelectKnotPath.reset()
    val curPoint = curKnotsList.getOrNull(curKnotIdx)
    curPoint?.let {
      val fixSize = mDefaultKnotRadius*2
      val rectangle =
        RectF(it.x - fixSize, it.y - fixSize, it.x + fixSize, it.y + fixSize)
      curSelectKnotPath.addRect(rectangle,CW)
    }
  }

  private fun drawBg(canvas: Canvas?) {
    val rect = Rect(mStartX, mStartY, mEndX, mEndY)
    mBgPaint.color = mBgColor
    canvas?.drawRect(rect, mBgPaint)
  }

  private fun drawBgLine(canvas: Canvas?) {
    mBgPaint.color = mBgLineColor
    mBgPaint.strokeWidth = mBgLineWidth
    val dx = (mWidth - 2 * mFixSize) / mBgLineCount
    val dy = (mHeight - 2 * mFixSize) / mBgLineCount
    for (i in 1 until mBgLineCount) {
      canvas?.drawLine(
          (mStartX + dx * i).toFloat(), mStartY.toFloat(), (mStartX + dx * i).toFloat(),
          mEndY.toFloat(), mBgPaint
      )
      canvas?.drawLine(
          mStartX.toFloat(), (mStartY + dy * i).toFloat(), mEndX.toFloat(),
          (mStartY + dy * i).toFloat(), mBgPaint
      )
    }
  }

  private fun drawUnselectedSpLine(canvas: Canvas?) {
    if (curSpType != ColorSpLineType.RGB){
      rgbPath = Path()
      setSplinePath(rgbPath,rgbKnotsList,false)
    }
    if (curSpType != ColorSpLineType.RED){
      redPath = Path()
      setSplinePath(redPath,redKnotsList,false)
    }
    if (curSpType != ColorSpLineType.GREEN){
      greenPath = Path()
      setSplinePath(greenPath,greenKnotsList,false)
    }
    if (curSpType != ColorSpLineType.BLUE){
      bluePath = Path()
      setSplinePath(bluePath,blueKnotsList,false)
    }
    when (curSpType) {
      ColorSpLineType.RGB -> {
        mCurvePaint.color = resources.getColor(R.color.color_33ff443b)
        canvas?.drawPath(redPath,mCurvePaint)
        mCurvePaint.color = resources.getColor(R.color.color_3384F767)
        canvas?.drawPath(greenPath,mCurvePaint)
        mCurvePaint.color = resources.getColor(R.color.color_333261ff)
        canvas?.drawPath(bluePath,mCurvePaint)
      }
      ColorSpLineType.RED -> {
        mCurvePaint.color = resources.getColor(R.color.color_33e0e0e0)
        canvas?.drawPath(rgbPath,mCurvePaint)
        mCurvePaint.color = resources.getColor(R.color.color_3384F767)
        canvas?.drawPath(greenPath,mCurvePaint)
        mCurvePaint.color = resources.getColor(R.color.color_333261ff)
        canvas?.drawPath(bluePath,mCurvePaint)
      }
      ColorSpLineType.GREEN -> {
        mCurvePaint.color = resources.getColor(R.color.color_33e0e0e0)
        canvas?.drawPath(rgbPath,mCurvePaint)
        mCurvePaint.color = resources.getColor(R.color.color_33ff443b)
        canvas?.drawPath(redPath,mCurvePaint)
        mCurvePaint.color = resources.getColor(R.color.color_333261ff)
        canvas?.drawPath(bluePath,mCurvePaint)
      }
      ColorSpLineType.BLUE -> {
        mCurvePaint.color = resources.getColor(R.color.color_33e0e0e0)
        canvas?.drawPath(rgbPath,mCurvePaint)
        mCurvePaint.color = resources.getColor(R.color.color_33ff443b)
        canvas?.drawPath(redPath,mCurvePaint)
        mCurvePaint.color = resources.getColor(R.color.color_3384F767)
        canvas?.drawPath(greenPath,mCurvePaint)
      }
    }
  }

  private fun drawSelectedSpline(canvas: Canvas?) {
    curRectPath.reset()
    setSplinePath(curPath, curKnotsList,true)
    mCurvePaint.color = mSelectedCurveLineColor
    canvas?.drawPath(curPath, mCurvePaint)
  }

  private fun drawCtrlKnot(canvas: Canvas?) {
    for (i in curKnotsList.indices) {
      if(i != curKnotIdx){
        mKnotPaint.color = mDefaultKnotColor
        canvas?.drawCircle(curKnotsList[i].x, curKnotsList[i].y, mDefaultKnotRadius.toFloat(), mKnotPaint)
      }
    }
    if (curKnotIdx != -1){
      mKnotPaint.color = mKnotStrokeColor
      canvas?.drawCircle(curKnotsList[curKnotIdx].x, curKnotsList[curKnotIdx].y, mSelectedKnotRadius.toFloat()+mKnotStrokeWidth, mKnotPaint)
      mKnotPaint.color = mSelectedKnotColor
      canvas?.drawCircle(curKnotsList[curKnotIdx].x, curKnotsList[curKnotIdx].y, mSelectedKnotRadius.toFloat(), mKnotPaint)
    }
  }

  private fun drawSelectPointText(canvas: Canvas?) {
    val curPoint = curKnotsList.getOrNull(curKnotIdx)
    curPoint?.let {
      val x = 255 * ((curPoint.x - mStartX) / (mWidth - 2*mFixSize))
      val y = 255 * ((mEndY - curPoint.y) / (mHeight - 2*mFixSize))
      val text = x.toInt().toString() + "," + y.toInt()
      val posx = mStartX + DPUtils.dpToPixel(context, 20).toFloat()
      val posy = mStartY + DPUtils.dpToPixel(context, 15).toFloat()
      canvas?.drawText(text,posx,posy,mTextPaint)
    }
  }

  private fun getFixArrayPoints(list: LinkedList<PointF>) : ArrayList<Point>{
    val qpoints = ArrayList<Point>()
    for(pointf in list){
      val x = 255 * ((pointf.x - mStartX) / (mWidth - 2*mFixSize))
      val y = 255 * ((mEndY - pointf.y) / (mHeight - 2*mFixSize))
      qpoints.add(Point(x.toInt(),y.toInt()))
    }
    return qpoints
  }

  /**
   * @param moveY y方向平移
   */
  private fun setSplinePath(
    path: Path?,
    points: LinkedList<PointF>,
    isSetSelect : Boolean
  ) {
    path?.reset()
    val length: Int = points.size
    val arrayX = DoubleArray(points.size)
    val arrayY = DoubleArray(points.size)
    for (n in 0 until length) {
      arrayX[n] = points[n].x.toDouble()
      arrayY[n] = points[n].y.toDouble()
    }
    path?.moveTo(mStartX.toFloat(), arrayY[0].toFloat())
    path?.lineTo(arrayX[0].toFloat(), arrayY[0].toFloat())
    val fixSize = mDefaultKnotRadius.toFloat()
    if( length <=1 ){
      return
    }
    if (length > 2) {
      sip.interpolate(arrayX, arrayY)
      val totalPoints = 200
      val dx = ((arrayX[length - 1] - arrayX[0]) / totalPoints)
      var m = 0
      while (m < totalPoints) {
        val curX = arrayX[0] + m * dx
        var curY = sip.value(curX.toDouble()).toFloat()
        curY = limitY(curY)
        if (m % 3 == 0 && isSetSelect) {
          curRectPath.addRect(
              RectF(
                  curX.toFloat() - fixSize, (curY ?: 0f) - fixSize, curX.toFloat() + fixSize,
                  (curY ?: 0f) + fixSize
              ), CW
          )
        }
        path?.lineTo(curX.toFloat(), curY ?: 0f)
        m++
      }
    }else if(length == 2){
      if (isSetSelect){
        curRectPath.moveTo(points[0].x-fixSize,points[0].y-fixSize)
        curRectPath.lineTo(points[1].x-fixSize,points[1].y-fixSize)
        curRectPath.lineTo(points[1].x+fixSize,points[1].y+fixSize)
        curRectPath.lineTo(points[0].x+fixSize,points[0].y+fixSize)
        curRectPath.close()
        curRectPath.moveTo(points[0].x+fixSize,points[0].y-fixSize)
        curRectPath.lineTo(points[1].x+fixSize,points[1].y-fixSize)
        curRectPath.lineTo(points[1].x-fixSize,points[1].y+fixSize)
        curRectPath.lineTo(points[0].x-fixSize,points[0].y+fixSize)
        curRectPath.close()
      }
    }
    if(isSetSelect){
      curRectPath.addRect(
          RectF(
              mStartX.toFloat(), points[0].y - fixSize, points[0].x, points[0].y + fixSize
          ), CW
      )
      curRectPath.addRect(
          RectF(
              points[length - 1].x, points[length - 1].y - fixSize, mEndX.toFloat(),
              points[length - 1].y + fixSize
          ), CW
      )
    }
    path?.lineTo(
        arrayX[points.size - 1].toFloat(), arrayY[points.size - 1].toFloat()
    )
    path?.lineTo(
        mEndX.toFloat(), arrayY[points.size - 1].toFloat()
    )
  }

  private fun limitX(pX: Float): Float {
    return when {
      pX > mEndX -> (mEndX).toFloat()
      pX < mStartX -> (mStartX).toFloat()
      else -> pX
    }
  }

  private fun limitY(pY: Float): Float {
    return when {
      pY > mEndY -> (mEndY).toFloat()
      pY < mStartY -> (mStartY).toFloat()
      else -> pY
    }
  }

  private fun isPointInCurSpPath(
    x: Float,
    y: Float
  ): Boolean {
    for (i in curKnotsList.indices) {
      val fixSize = mSelectedKnotRadius
      val knotPath = Path()
      knotPath.moveTo(curKnotsList[i].x, curKnotsList[i].y)
      val knotRectangle =
        RectF(
            curKnotsList[i].x - fixSize, curKnotsList[i].y - fixSize, curKnotsList[i].x + fixSize,
            curKnotsList[i].y + fixSize
        )
      knotPath.addRect(knotRectangle, CW)
      if(isPointAroundPath(x, y,knotPath)){
        return true
      }
    }
    return isPointAroundPath(x, y,curRectPath)
  }

  fun isPointAroundPath(
    x: Float,
    y: Float,
    path: Path?
  ): Boolean {
    val fixSize = DPUtils.dpToPixel(context, 9)
    val tempPath = Path()
    tempPath.moveTo(x, y)
    val rectangle =
      RectF(x - fixSize, y - fixSize, x + fixSize, y + fixSize)
    tempPath.addRect(rectangle, CW)
    tempPath.op(path, INTERSECT)
    return !tempPath.isEmpty
  }

  private fun addNewKnot(
    x: Float,
    y: Float
  ) {
    for (i in curKnotsList.indices) {
      val fixSize = mSelectedKnotRadius
      val knotPath = Path()
      val tempPath = Path()
      knotPath.moveTo(curKnotsList[i].x, curKnotsList[i].y)
      tempPath.moveTo(x, y)
      val rectangle =
        RectF(x - fixSize, y - fixSize, x + fixSize, y + fixSize)
      val knotRectangle =
        RectF(
            curKnotsList[i].x - fixSize, curKnotsList[i].y - fixSize, curKnotsList[i].x + fixSize,
            curKnotsList[i].y + fixSize
        )
      tempPath.addRect(rectangle, CW)
      knotPath.addRect(knotRectangle, CW)
      tempPath.op(knotPath, INTERSECT)
      if (!tempPath.isEmpty) { //判断触摸点是否在已添加控制点的附近
        if (curKnotIdx != i){
          selectNewPoint = true
        }
        curKnotIdx = i
        return
      } else { //新增控制点
        val fixSize = minKnotsSpacing
        val maxX = curKnotsList.last
            .x
        val minX = curKnotsList.first
            .x
        if (x > maxX + fixSize) { //插入到队尾
          selectNewPoint = true
          curKnotsList.add(PointF(x,curKnotsList.last.y))
          curKnotIdx = curKnotsList.size - 1
          return
        }

        if (x < minX - fixSize) { //插入到队头
          selectNewPoint = true
          curKnotsList.addFirst(PointF(x,curKnotsList.first.y))
          curKnotIdx = 0
          return
        }

        val preX = curKnotsList[i]
            .x
        val nextPoint = curKnotsList.getOrNull(i+1) ?: continue
        val nextX = nextPoint.x
        if (x in preX..nextX) {
          if ( x > (preX + fixSize) && x < (nextX - fixSize)) { //插入队中间
            selectNewPoint = true
            if (curKnotsList.size == 2){
              val k = (curKnotsList[1].y - curKnotsList[0].y) / (curKnotsList[1].x - curKnotsList[0].x)
              val a = curKnotsList[0].y - k * curKnotsList[0].x
              curKnotsList.add(i+1, PointF(x,k*x + a))
            }else {
              var insertY = sip.value(x.toDouble()).toFloat()
              if(insertY <= mStartY){
                insertY = mStartY.toFloat()
              }
              if(insertY >= mEndY){
                insertY = mEndY.toFloat()
              }
              if(insertY <= mStartY){
                insertY = mStartY.toFloat()
              }
              if(insertY >= mEndY){
                insertY = mEndY.toFloat()
              }
              curKnotsList.add(i+1, PointF(x,insertY))
            }
            curKnotIdx = i+1
          }
        }
      }
    }
  }

  fun deleteCurKnot(){
    curKnotsList.removeAt(curKnotIdx)
    curKnotIdx = -1
    mCallBack?.onUpdate(getFixArrayPoints(curKnotsList),curSpType)
  }

  fun setOnCtrPointsUpdateCallBack(callBack: OnCtrPointsUpdateCallBack){
    this.mCallBack = callBack
  }

  interface OnCtrPointsUpdateCallBack{
    fun onUpdate(points: ArrayList<Point>,type:ColorSpLineType)
  }

  inner class GestureListener : SimpleOnGestureListener() {

    override fun onSingleTapUp(e: MotionEvent): Boolean {
      if(isPointAroundPath(e.x,e.y,curSelectKnotPath)){
        onGestureIntercept = true
      }
      return super.onSingleTapUp(e)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
      if (e != null){
        if (curKnotIdx != -1 && isPointAroundPath(e.x,e.y,curSelectKnotPath) && curKnotsList.size > 2){
          deleteCurKnot()
          onGestureIntercept = true
          invalidate()
        }
      }
      return super.onDoubleTap(e)
    }
  }

}