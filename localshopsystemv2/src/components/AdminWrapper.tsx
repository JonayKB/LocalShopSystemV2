import React from 'react'
import Basket from './Basket'
import Navbar from './Navbar'
import { Outlet } from 'react-router-dom'

type Props = {}

const AdminWrapper = (props: Props) => {
    return (
        <>
            <Basket />
            <Navbar />
            <Outlet />
        </>
    )
}

export default AdminWrapper