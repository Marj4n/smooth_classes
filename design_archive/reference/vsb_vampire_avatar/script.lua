--squapi
vanilla_model.PLAYER:setVisible(false)

local squapi = require("SquAPI")

squapi.smoothHead:new(
    {
        models.model.root.head --element(you can have multiple elements in a table)
    },
    {
        1 --(1) strength(you can have multiple strengths in a table)
    },
    0.1,    --(0.1) tilt
    1,    --(1) speed
    true     --(true) keepOriginalHeadPos
)


function events.render()
    local pose = player:getPose()
    if player:getPose() == "CROUCHING" then
        models.model.root.LeftLeg:setPos(0,0,-4.5)
        models.model.root.RightLeg:setPos(0,0,-4.5)
        models.model.root.head:setPos(0,-4,0)
    else
        models.model.root.LeftLeg:setPos(0,0,0)
        models.model.root.RightLeg:setPos(0,0,0)
        models.model.root.head:setPos(0,0,0)
    end
    
    if player:getVehicle() and player:getVehicle():getType() == "minecraft:horse" then
        models.model.root:setPos(0,-4,0)

-- POS change
end
    
end