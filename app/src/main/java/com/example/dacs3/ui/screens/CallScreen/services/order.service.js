package com.example.dacs3.ui.screens.CallScreen.services

// Đây là ví dụ dummy, bạn thay thế bằng DB thật (MongoDB, MySQL,...)

const orders = []; // mảng tạm lưu đơn hàng

const createOrder = async (data) => {
  const order = {
    id: String(Date.now()),
    amount: data.amount,
    status: 'pending',
    ...data,
  };
  orders.push(order);
  return order;
};

const updateOrderStatus = async (orderId, responseCode) => {
  const order = orders.find((o) => o.id === orderId);
  if (!order) throw new Error('Order not found');
  order.status = responseCode === '00' ? 'paid' : 'failed';
  return order;
};

export default {
  createOrder,
  updateOrderStatus,
};
