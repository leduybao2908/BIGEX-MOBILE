package com.example.dacs3.ui.screens.CallScreen.controllers

import orderService from '../services/order.service.js';
import vnpay from '../utils/vnpay.js';
import { ProductCode, VnpLocale, dateFormat } from 'vnpay';

const createOrder = async (req, res) => {
  try {
    // Tạo đơn hàng, bạn thay thế bằng logic lưu DB của bạn
    const orderData = req.body;
    const order = await orderService.createOrder(orderData);

    // Tạo URL thanh toán
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    const ipAddr =
      req.headers['x-forwarded-for'] ||
      req.connection.remoteAddress ||
      req.socket.remoteAddress ||
      req.ip;
    const amount = order.amount * 100; // BẮT BUỘC nhân 100
    const txnRef = `ORD_${Date.now()}`; // dùng timestamp làm mã đơn hàng đơn giản
    const paymentUrl = vnpay.buildPaymentUrl({
      vnp_Amount: order.amount,
      vnp_IpAddr: ipAddr,
      vnp_TxnRef: order.id,
      vnp_OrderInfo: `Thanh toan don hang ${order.id}`,
      vnp_OrderType: ProductCode.Other,
      vnp_ReturnUrl: 'https://5870-2001-ee1-db04-d6d0-c9ba-c1f3-3c32-a610.ngrok-free.app/api/vnpay-return',
      vnp_Locale: VnpLocale.VN,
      vnp_CreateDate: dateFormat(new Date()),
      vnp_ExpireDate: dateFormat(tomorrow),
    });

    return res.json({
      success: true,
      paymentUrl,
      order,
    });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

const vnpayReturn = async (req, res) => {
  try {
    const vnpData = req.query;

    // Xác thực dữ liệu trả về từ VNPay, thư viện vnpay có hàm verify (bạn tham khảo docs)
    const isValid = vnpay.verifyReturnUrl(vnpData);

    if (!isValid) {
      return res.status(400).send('Invalid checksum');
    }

    // Xử lý cập nhật trạng thái đơn hàng theo vnpData.vnp_TxnRef và vnpData.vnp_ResponseCode
    await orderService.updateOrderStatus(vnpData.vnp_TxnRef, vnpData.vnp_ResponseCode);

    // Trả về cho người dùng (hoặc redirect sang trang kết quả)
    return res.send('Thanh toán thành công');
  } catch (error) {
    console.error(error);
    return res.status(500).send('Lỗi xử lý thanh toán');
  }
};

export default {
  createOrder,
  vnpayReturn,
};
