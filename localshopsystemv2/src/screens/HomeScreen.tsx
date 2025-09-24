import React from 'react'
import { Link } from 'react-router-dom'

type Props = {}

const HomeScreen = (props: Props) => {
  return (
    <div>
      <Link to={"admin/Home"} >Ir a ADMIN</Link>
    </div>
  )
}

export default HomeScreen